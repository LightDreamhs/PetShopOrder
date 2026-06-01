package com.petshop.order.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.order.entity.OrderItem;
import com.petshop.order.entity.Orders;
import com.petshop.order.entity.SystemConfig;
import com.petshop.order.mapper.SystemConfigMapper;
import com.petshop.order.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.aes-key:PetShop2026Order!}")
    private String aesKey;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_RETRIES = 2;
    private static final long[] RETRY_DELAYS_MS = {3000L, 6000L};

    @Async
    @Override
    public void sendNewOrderNotification(Orders order, List<OrderItem> items) {
        try {
            SystemConfig config = systemConfigMapper.selectById(1L);
            if (config == null || config.getHasQywxWebhook() == null || config.getHasQywxWebhook() != 1) {
                return;
            }
            if (config.getQywxWebhookUrlEnc() == null) {
                return;
            }

            AES aes = SecureUtil.aes(aesKey.getBytes());
            String webhookUrl = aes.decryptStr(config.getQywxWebhookUrlEnc());
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

            String maskedPhone = maskPhone(order.getCustomerPhoneSnapshot());
            String memberInfo = order.getMemberLevelSnapshot() != null ? order.getMemberLevelSnapshot() : "非会员";
            String deliveryInfo = order.getNeedDelivery() == 1
                    ? "是 (" + formatDistance(order.getDeliveryDistance()) + ")"
                    : "否";
            String goodsSummary = items.stream()
                    .map(item -> item.getProductName() + (item.getSkuName() != null ? " " + item.getSkuName() : "") + " x" + item.getQuantity())
                    .collect(Collectors.joining(", "));
            String timeStr = order.getCreateTime() != null ? order.getCreateTime().format(FMT) : "";

            String jsonBody;
            if (isFeishu(webhookUrl)) {
                jsonBody = buildFeishuCard(order, maskedPhone, memberInfo, deliveryInfo, goodsSummary, timeStr);
            } else {
                jsonBody = buildQywxMarkdown(order, maskedPhone, memberInfo, deliveryInfo, goodsSummary, timeStr);
            }
            if (jsonBody == null) {
                return;
            }

            RestTemplate restTemplate = new RestTemplate();
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                try {
                    RequestEntity<String> request = RequestEntity
                            .post(new URI(webhookUrl))
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
                            .body(jsonBody);
                    String response = restTemplate.exchange(request, String.class).getBody();
                    if (isSendSuccess(response, webhookUrl)) {
                        return;
                    }
                    log.warn("Webhook 返回失败, orderNo={}, attempt={}, response={}", order.getOrderNo(), attempt + 1, response);
                } catch (Exception sendEx) {
                    log.warn("Webhook 发送异常, orderNo={}, attempt={}", order.getOrderNo(), attempt + 1, sendEx.getMessage());
                }
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                }
            }
            log.error("Webhook 发送最终失败（已重试{}次）, orderNo={}", MAX_RETRIES, order.getOrderNo());
        } catch (Exception e) {
            log.error("发送新订单通知失败, orderNo={}", order.getOrderNo(), e);
        }
    }

    private boolean isFeishu(String url) {
        return url.contains("open.feishu.cn");
    }

    private boolean isSendSuccess(String response, String webhookUrl) {
        try {
            JsonNode node = objectMapper.readTree(response);
            if (isFeishu(webhookUrl)) {
                return node.has("StatusCode") && node.get("StatusCode").asInt(-1) == 0;
            }
            return node.has("errcode") && node.get("errcode").asInt(-1) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildFeishuCard(Orders order, String maskedPhone, String memberInfo,
                                    String deliveryInfo, String goodsSummary, String timeStr) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            Map<String, Object> title = new LinkedHashMap<>();
            title.put("tag", "plain_text");
            title.put("content", "新订单通知");
            header.put("title", title);
            header.put("template", "turquoise");

            StringBuilder md = new StringBuilder();
            md.append("**订单号**: ").append(order.getOrderNo()).append("\n");
            md.append("**客户**: ").append(maskedPhone).append("\n");
            md.append("**会员**: ").append(memberInfo).append("\n");
            md.append("**金额**: ¥").append(order.getTotalAmount().toPlainString()).append("\n");
            md.append("**配送**: ").append(deliveryInfo).append("\n");
            md.append("**商品**: ").append(goodsSummary).append("\n");
            md.append("**时间**: ").append(timeStr);

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("tag", "lark_md");
            text.put("content", md.toString());

            Map<String, Object> element = new LinkedHashMap<>();
            element.put("tag", "div");
            element.put("text", text);

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("header", header);
            card.put("elements", Collections.singletonList(element));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msg_type", "interactive");
            body.put("card", card);

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("构建飞书消息失败", e);
            return null;
        }
    }

    private String buildQywxMarkdown(Orders order, String maskedPhone, String memberInfo,
                                      String deliveryInfo, String goodsSummary, String timeStr) {
        try {
            String content = "### 新订单通知\n"
                    + "**订单号**: " + order.getOrderNo() + "\n"
                    + "**客户**: " + maskedPhone + "\n"
                    + "**会员**: " + memberInfo + "\n"
                    + "**金额**: ¥" + order.getTotalAmount().toPlainString() + "\n"
                    + "**配送**: " + deliveryInfo + "\n"
                    + "**商品**: " + goodsSummary + "\n"
                    + "**时间**: " + timeStr;

            Map<String, Object> markdown = new LinkedHashMap<>();
            markdown.put("content", content);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "markdown");
            body.put("markdown", markdown);

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("构建企微消息失败", e);
            return null;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String formatDistance(Integer meter) {
        if (meter == null) {
            return "未知";
        }
        if (meter < 1000) {
            return meter + "m";
        }
        double km = meter / 1000.0;
        if (km == Math.floor(km)) {
            return (long) km + "km";
        }
        return String.format("%.1fkm", km);
    }
}
