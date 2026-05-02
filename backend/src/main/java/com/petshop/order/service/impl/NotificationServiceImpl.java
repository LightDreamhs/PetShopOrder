package com.petshop.order.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.petshop.order.entity.OrderItem;
import com.petshop.order.entity.Orders;
import com.petshop.order.entity.SystemConfig;
import com.petshop.order.mapper.SystemConfigMapper;
import com.petshop.order.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemConfigMapper systemConfigMapper;

    @Value("${app.webhook.aes-key:PetShop2026Order!}")
    private String aesKey;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject(webhookUrl, body, String.class);
        } catch (Exception e) {
            log.error("发送新订单通知失败, orderNo={}", order.getOrderNo(), e);
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
