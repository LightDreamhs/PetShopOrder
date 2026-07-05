package com.petshop.order.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.order.entity.OrderItem;
import com.petshop.order.entity.Orders;
import com.petshop.order.entity.SystemConfig;
import com.petshop.order.mapper.SystemConfigMapper;
import com.petshop.order.service.NotificationService;
import com.petshop.order.service.dto.AppointmentNotifyInfo;
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
import java.security.MessageDigest;
import java.time.LocalDateTime;
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

    private byte[] deriveAesKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(aesKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("AES 密钥派生失败", e);
        }
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_RETRIES = 2;
    private static final long[] RETRY_DELAYS_MS = {3000L, 6000L};

    @Async
    @Override
    public void sendNewOrderNotification(Orders order, List<OrderItem> items) {
        sendNewOrderNotification(order, items, null);
    }

    @Async
    @Override
    public void sendNewOrderNotification(Orders order, List<OrderItem> items, AppointmentNotifyInfo appointmentInfo) {
        try {
            SystemConfig config = systemConfigMapper.selectById(1L);
            if (config == null || config.getHasQywxWebhook() == null || config.getHasQywxWebhook() != 1) {
                return;
            }
            if (config.getQywxWebhookUrlEnc() == null) {
                return;
            }

            AES aes = SecureUtil.aes(deriveAesKey());
            String webhookUrl = aes.decryptStr(config.getQywxWebhookUrlEnc());
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

            String phone = order.getCustomerPhoneSnapshot();
            String customerName = order.getCustomerName();
            String memberInfo = order.getMemberLevelSnapshot() != null ? order.getMemberLevelSnapshot() : "非会员";
            String deliveryInfo = order.getNeedDelivery() == 1
                    ? "是 (" + formatDistance(order.getDeliveryDistance()) + ")"
                    : "否";
            String deliveryAddress = order.getDeliveryAddress();
            String remark = order.getRemark();

            // 商品列表：每行一个，加粗显示
            String goodsList = items.stream()
                    .map(item -> "　**" + item.getProductName()
                            + (item.getSkuName() != null ? " " + item.getSkuName() : "")
                            + "** x" + item.getQuantity())
                    .collect(Collectors.joining("\n"));

            // 时间：优先用数据库值，为空则取当前时间
            String timeStr = order.getCreateTime() != null
                    ? order.getCreateTime().format(FMT)
                    : LocalDateTime.now().format(FMT);

            String jsonBody;
            if (isFeishu(webhookUrl)) {
                jsonBody = buildFeishuCard(order, phone, customerName, memberInfo,
                        deliveryInfo, deliveryAddress, goodsList, remark, timeStr, appointmentInfo);
            } else {
                jsonBody = buildQywxMarkdown(order, phone, customerName, memberInfo,
                        deliveryInfo, deliveryAddress, goodsList, remark, timeStr, appointmentInfo);
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

    private String buildFeishuCard(Orders order, String phone, String customerName,
                                    String memberInfo, String deliveryInfo,
                                    String deliveryAddress, String goodsList,
                                    String remark, String timeStr, AppointmentNotifyInfo appointmentInfo) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            Map<String, Object> title = new LinkedHashMap<>();
            title.put("tag", "plain_text");
            title.put("content", appointmentInfo != null ? "📅 新预约通知" : "🎉 新订单通知");
            header.put("title", title);
            header.put("template", appointmentInfo != null ? "orange" : "turquoise");

            StringBuilder md = new StringBuilder();
            md.append("**订单号**: ").append(order.getOrderNo()).append("\n");
            md.append("**联系人**: ").append(customerName != null ? customerName : "未填写").append("\n");
            md.append("**电话**: ").append(phone != null ? phone : "未知").append("\n");
            md.append("**会员**: ").append(memberInfo).append("\n");
            md.append("**金额**: ¥").append(order.getTotalAmount().toPlainString()).append("\n");
            // 预约信息块（仅预约订单展示）
            if (appointmentInfo != null) {
                md.append("**预约时间**: ").append(appointmentInfo.getStartTime().format(FMT))
                        .append(" ~ ").append(appointmentInfo.getEndTime().format(FMT))
                        .append("（").append(appointmentInfo.getTotalDuration()).append("分钟）\n");
                if (appointmentInfo.getPetInfo() != null && !appointmentInfo.getPetInfo().isEmpty()) {
                    md.append("**宠物**: ").append(appointmentInfo.getPetInfo()).append("\n");
                }
            }
            md.append("**配送**: ").append(deliveryInfo);
            if (order.getNeedDelivery() == 1 && deliveryAddress != null && !deliveryAddress.isEmpty()) {
                md.append("\n**地址**: ").append(deliveryAddress);
            }
            md.append("\n**商品**:\n").append(goodsList).append("\n");
            md.append("**时间**: ").append(timeStr);
            if (remark != null && !remark.isEmpty()) {
                md.append("\n**备注**: ").append(remark);
            }

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

    private String buildQywxMarkdown(Orders order, String phone, String customerName,
                                      String memberInfo, String deliveryInfo,
                                      String deliveryAddress, String goodsList,
                                      String remark, String timeStr, AppointmentNotifyInfo appointmentInfo) {
        try {
            StringBuilder content = new StringBuilder();
            content.append(appointmentInfo != null ? "### 📅 新预约通知\n" : "### 🎉 新订单通知\n");
            content.append("**订单号**: ").append(order.getOrderNo()).append("\n");
            content.append("**联系人**: ").append(customerName != null ? customerName : "未填写").append("\n");
            content.append("**电话**: ").append(phone != null ? phone : "未知").append("\n");
            content.append("**会员**: ").append(memberInfo).append("\n");
            content.append("**金额**: ¥").append(order.getTotalAmount().toPlainString()).append("\n");
            // 预约信息块（仅预约订单展示）
            if (appointmentInfo != null) {
                content.append("**预约时间**: ").append(appointmentInfo.getStartTime().format(FMT))
                        .append(" ~ ").append(appointmentInfo.getEndTime().format(FMT))
                        .append("（").append(appointmentInfo.getTotalDuration()).append("分钟）\n");
                if (appointmentInfo.getPetInfo() != null && !appointmentInfo.getPetInfo().isEmpty()) {
                    content.append("**宠物**: ").append(appointmentInfo.getPetInfo()).append("\n");
                }
            }
            content.append("**配送**: ").append(deliveryInfo);
            if (order.getNeedDelivery() == 1 && deliveryAddress != null && !deliveryAddress.isEmpty()) {
                content.append("\n**地址**: ").append(deliveryAddress);
            }
            content.append("\n**商品**:\n").append(goodsList).append("\n");
            content.append("**时间**: ").append(timeStr);
            if (remark != null && !remark.isEmpty()) {
                content.append("\n**备注**: ").append(remark);
            }

            Map<String, Object> markdown = new LinkedHashMap<>();
            markdown.put("content", content.toString());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "markdown");
            body.put("markdown", markdown);

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("构建企微消息失败", e);
            return null;
        }
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
