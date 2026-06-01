package com.petshop.order.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.entity.SystemConfig;
import com.petshop.order.entity.SystemConfigDeliveryTier;
import com.petshop.order.entity.SystemConfigLog;
import com.petshop.order.mapper.SystemConfigDeliveryTierMapper;
import com.petshop.order.mapper.SystemConfigLogMapper;
import com.petshop.order.mapper.SystemConfigMapper;
import com.petshop.order.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Long CONFIG_ID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SystemConfigMapper systemConfigMapper;
    private final SystemConfigDeliveryTierMapper tierMapper;
    private final SystemConfigLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.aes-key:PetShop2026Order!}")
    private String aesKey;

    @Override
    public Map<String, Object> getConfig() {
        SystemConfig config = getOrCreateConfig();
        List<SystemConfigDeliveryTier> tiers = tierMapper.selectByConfigId(CONFIG_ID);
        List<SystemConfigLog> logs = logMapper.selectRecentByConfigId(CONFIG_ID, 20);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("config", buildConfigMap(config, tiers));
        result.put("changeLogs", logs.stream().map(this::buildLogMap).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateConfig(Map<String, Object> params, AdminUser operator) {
        SystemConfig config = getOrCreateConfig();
        String beforeJson = toJson(config);
        String summary = buildSummary(params, config);

        if (params.containsKey("shopLat")) {
            config.setShopLat(toBigDecimal(params.get("shopLat")));
        }
        if (params.containsKey("shopLng")) {
            config.setShopLng(toBigDecimal(params.get("shopLng")));
        }
        if (params.containsKey("deliveryRadiusKm")) {
            BigDecimal radius = toBigDecimal(params.get("deliveryRadiusKm"));
            if (radius == null || radius.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("配送半径必须大于 0");
            }
            config.setDeliveryRadiusKm(radius);
        }
        if (params.containsKey("deliveryMinAmount")) {
            config.setDeliveryMinAmount(toBigDecimal(params.get("deliveryMinAmount")));
        }
        if (params.containsKey("deliveryFeeType")) {
            String feeType = (String) params.get("deliveryFeeType");
            if (!"FREE".equals(feeType) && !"TIERED".equals(feeType)) {
                throw new BusinessException("配送费类型只支持 FREE 或 TIERED");
            }
            config.setDeliveryFeeType(feeType);
        }
        if (params.containsKey("fixedDeliveryFee")) {
            config.setFixedDeliveryFee(toBigDecimal(params.get("fixedDeliveryFee")));
        }
        if (params.containsKey("orderTimeEnabled")) {
            config.setOrderTimeEnabled(toBoolean(params.get("orderTimeEnabled")) ? 1 : 0);
        }
        if (params.containsKey("orderStartTime")) {
            config.setOrderStartTime(parseTime(params.get("orderStartTime")));
        }
        if (params.containsKey("orderEndTime")) {
            config.setOrderEndTime(parseTime(params.get("orderEndTime")));
        }

        if (params.containsKey("qywxWebhookUrl")) {
            String webhookUrl = (String) params.get("qywxWebhookUrl");
            if (webhookUrl == null || webhookUrl.contains("key=***")) {
                // null 或前端回传脱敏值 = 不修改
            } else if (webhookUrl.isEmpty()) {
                config.setQywxWebhookUrlEnc(null);
                config.setHasQywxWebhook(0);
            } else {
                validateWebhookUrl(webhookUrl);
                AES aes = SecureUtil.aes(aesKey.getBytes());
                config.setQywxWebhookUrlEnc(aes.encrypt(webhookUrl));
                config.setHasQywxWebhook(1);
            }
        }

        if (params.containsKey("paymentQrUrl")) {
            config.setPaymentQrUrl((String) params.get("paymentQrUrl"));
        }

        config.setUpdatedBy(operator.getId());
        systemConfigMapper.updateById(config);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tierRules = (List<Map<String, Object>>) params.get("tieredDeliveryFeeRules");
        if (tierRules != null && "TIERED".equals(config.getDeliveryFeeType())) {
            validateTierRules(tierRules, config.getDeliveryRadiusKm());
            tierMapper.deleteByConfigId(CONFIG_ID);
            if (!tierRules.isEmpty()) {
                List<SystemConfigDeliveryTier> tiers = new ArrayList<>();
                for (int i = 0; i < tierRules.size(); i++) {
                    Map<String, Object> rule = tierRules.get(i);
                    SystemConfigDeliveryTier tier = new SystemConfigDeliveryTier();
                    tier.setConfigId(CONFIG_ID);
                    tier.setMinDistanceKm(toBigDecimal(rule.get("minDistanceKm")));
                    tier.setMaxDistanceKm(toBigDecimal(rule.get("maxDistanceKm")));
                    tier.setFee(toBigDecimal(rule.get("fee")));
                    tier.setSort(i);
                    tiers.add(tier);
                }
                tierMapper.insertBatch(tiers);
            }
        }

        SystemConfigLog configLog = new SystemConfigLog();
        configLog.setConfigId(CONFIG_ID);
        configLog.setOperatorId(operator.getId());
        configLog.setOperatorName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
        configLog.setSummary(summary);
        configLog.setBeforeVal(beforeJson);
        configLog.setAfterVal(toJson(config));
        logMapper.insert(configLog);

        List<SystemConfigDeliveryTier> updatedTiers = tierMapper.selectByConfigId(CONFIG_ID);
        return buildConfigMap(config, updatedTiers);
    }

    @Override
    public Map<String, Object> testWebhook(String webhookUrl) {
        validateWebhookUrl(webhookUrl);
        try {
            String body;
            if (webhookUrl.contains("open.feishu.cn")) {
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("text", "PetShop 系统配置 Webhook 测试消息");
                Map<String, Object> bodyMap = new LinkedHashMap<>();
                bodyMap.put("msg_type", "text");
                bodyMap.put("content", content);
                body = objectMapper.writeValueAsString(bodyMap);
            } else {
                Map<String, Object> text = new LinkedHashMap<>();
                text.put("content", "PetShop 系统配置 Webhook 测试消息");
                Map<String, Object> bodyMap = new LinkedHashMap<>();
                bodyMap.put("msgtype", "text");
                bodyMap.put("text", text);
                body = objectMapper.writeValueAsString(bodyMap);
            }
            String response = cn.hutool.http.HttpRequest.post(webhookUrl)
                    .body(body, "application/json;charset=UTF-8")
                    .execute()
                    .body();
            return Map.of("success", true, "response", response);
        } catch (Exception e) {
            throw new BusinessException("Webhook 测试发送失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getShopLocation() {
        SystemConfig config = getOrCreateConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shopLat", config.getShopLat());
        result.put("shopLng", config.getShopLng());
        return result;
    }

    @Override
    public Map<String, Object> getDeliveryConfig() {
        SystemConfig config = getOrCreateConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deliveryRadiusKm", config.getDeliveryRadiusKm());
        result.put("deliveryMinAmount", config.getDeliveryMinAmount());
        result.put("deliveryFeeType", config.getDeliveryFeeType());
        result.put("fixedDeliveryFee", config.getFixedDeliveryFee());
        result.put("orderTimeEnabled", config.getOrderTimeEnabled());
        result.put("orderStartTime", config.getOrderStartTime() != null ? config.getOrderStartTime().format(TIME_FMT) : null);
        result.put("orderEndTime", config.getOrderEndTime() != null ? config.getOrderEndTime().format(TIME_FMT) : null);
        return result;
    }

    @Override
    public List<Map<String, Object>> getTierRules(Long configId) {
        List<SystemConfigDeliveryTier> tiers = tierMapper.selectByConfigId(configId);
        return tiers.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("minDistanceKm", t.getMinDistanceKm() != null ? t.getMinDistanceKm().toPlainString() : null);
            m.put("maxDistanceKm", t.getMaxDistanceKm() != null ? t.getMaxDistanceKm().toPlainString() : null);
            m.put("fee", t.getFee() != null ? t.getFee().toPlainString() : null);
            m.put("sort", t.getSort());
            return m;
        }).collect(Collectors.toList());
    }

    private SystemConfig getOrCreateConfig() {
        SystemConfig config = systemConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new SystemConfig();
            config.setId(CONFIG_ID);
            config.setDeliveryRadiusKm(new BigDecimal("5.00"));
            config.setDeliveryMinAmount(new BigDecimal("20.00"));
            config.setDeliveryFeeType("FREE");
            config.setFixedDeliveryFee(BigDecimal.ZERO);
            config.setOrderTimeEnabled(0);
            config.setHasQywxWebhook(0);
            systemConfigMapper.insert(config);
        }
        return config;
    }

    private Map<String, Object> buildConfigMap(SystemConfig config, List<SystemConfigDeliveryTier> tiers) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shopLat", config.getShopLat());
        m.put("shopLng", config.getShopLng());
        m.put("deliveryRadiusKm", config.getDeliveryRadiusKm());
        m.put("deliveryMinAmount", config.getDeliveryMinAmount() != null ? config.getDeliveryMinAmount().toPlainString() : null);
        m.put("deliveryFeeType", config.getDeliveryFeeType());
        m.put("fixedDeliveryFee", config.getFixedDeliveryFee() != null ? config.getFixedDeliveryFee().toPlainString() : null);
        m.put("tieredDeliveryFeeRules", tiers.stream().map(t -> {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("minDistanceKm", t.getMinDistanceKm() != null ? t.getMinDistanceKm().toPlainString() : null);
            rule.put("maxDistanceKm", t.getMaxDistanceKm() != null ? t.getMaxDistanceKm().toPlainString() : null);
            rule.put("fee", t.getFee() != null ? t.getFee().toPlainString() : null);
            return rule;
        }).collect(Collectors.toList()));
        m.put("orderTimeEnabled", config.getOrderTimeEnabled() != null && config.getOrderTimeEnabled() == 1);
        m.put("orderStartTime", config.getOrderStartTime() != null ? config.getOrderStartTime().format(TIME_FMT) : null);
        m.put("orderEndTime", config.getOrderEndTime() != null ? config.getOrderEndTime().format(TIME_FMT) : null);

        String maskedWebhook = null;
        if (config.getHasQywxWebhook() != null && config.getHasQywxWebhook() == 1 && config.getQywxWebhookUrlEnc() != null) {
            try {
                AES aes = SecureUtil.aes(aesKey.getBytes());
                String decrypted = aes.decryptStr(config.getQywxWebhookUrlEnc());
                maskedWebhook = maskWebhookKey(decrypted);
            } catch (Exception e) {
                maskedWebhook = null;
            }
        }
        m.put("qywxWebhookUrl", maskedWebhook);
        m.put("hasQywxWebhook", config.getHasQywxWebhook() != null && config.getHasQywxWebhook() == 1);
        m.put("paymentQrUrl", config.getPaymentQrUrl());

        String updatedByName = null;
        if (config.getUpdatedBy() != null) {
            updatedByName = String.valueOf(config.getUpdatedBy());
        }
        m.put("updatedBy", updatedByName);
        m.put("updatedAt", config.getUpdateTime() != null ? config.getUpdateTime().format(DATETIME_FMT) : null);
        return m;
    }

    private Map<String, Object> buildLogMap(SystemConfigLog log) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", log.getId());
        m.put("operatorName", log.getOperatorName());
        m.put("changedAt", log.getCreateTime() != null ? log.getCreateTime().format(DATETIME_FMT) : null);
        m.put("summary", log.getSummary());
        return m;
    }

    private static final Set<String> ALLOWED_WEBHOOK_HOSTS = Set.of(
            "qyapi.weixin.qq.com",
            "open.feishu.cn"
    );

    private void validateWebhookUrl(String url) {
        try {
            URI uri = new URI(url);
            if (!"https".equals(uri.getScheme())) {
                throw new BusinessException("非法的 Webhook 地址: 必须使用 HTTPS");
            }
            String host = uri.getHost();
            if (host == null || !ALLOWED_WEBHOOK_HOSTS.contains(host)) {
                throw new BusinessException("非法的 Webhook 地址: 仅允许 " + String.join("、", ALLOWED_WEBHOOK_HOSTS));
            }
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()) {
                throw new BusinessException("非法的 Webhook 地址: 禁止内网地址");
            }
            String hostIp = address.getHostAddress();
            if (hostIp.startsWith("10.")
                    || hostIp.startsWith("192.168.")
                    || hostIp.startsWith("0.")) {
                throw new BusinessException("非法的 Webhook 地址: 禁止内网地址");
            }
            if (hostIp.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
                throw new BusinessException("非法的 Webhook 地址: 禁止内网地址");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("非法的 Webhook 地址");
        }
    }

    private String maskWebhookKey(String url) {
        if (url == null) return null;
        if (url.contains("open.feishu.cn")) {
            return url.replaceAll("hook/[0-9a-f-]+", "hook/***");
        }
        return url.replaceAll("key=[^&]+", "key=***");
    }

    private void validateTierRules(List<Map<String, Object>> rules, BigDecimal radiusKm) {
        if (rules.isEmpty()) {
            throw new BusinessException("分段配送规则不能为空");
        }
        List<BigDecimal> boundaries = new ArrayList<>();
        for (Map<String, Object> rule : rules) {
            BigDecimal min = toBigDecimal(rule.get("minDistanceKm"));
            BigDecimal max = toBigDecimal(rule.get("maxDistanceKm"));
            if (min == null || max == null) {
                throw new BusinessException("分段规则的起止距离不能为空");
            }
            if (min.compareTo(max) >= 0) {
                throw new BusinessException("分段规则的最小距离必须小于最大距离");
            }
            boundaries.add(min);
            boundaries.add(max);
        }
        Collections.sort(boundaries);
        if (boundaries.get(0).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("分段规则必须从 0 km 开始");
        }
        if (boundaries.get(boundaries.size() - 1).compareTo(radiusKm) != 0) {
            throw new BusinessException("分段规则必须覆盖到配送半径 " + radiusKm + " km");
        }
        List<BigDecimal> sortedMins = rules.stream()
                .map(r -> toBigDecimal(r.get("minDistanceKm")))
                .sorted()
                .collect(Collectors.toList());
        List<BigDecimal> sortedMaxs = rules.stream()
                .map(r -> toBigDecimal(r.get("maxDistanceKm")))
                .sorted()
                .collect(Collectors.toList());
        for (int i = 1; i < sortedMins.size(); i++) {
            if (sortedMins.get(i).compareTo(sortedMaxs.get(i - 1)) != 0) {
                throw new BusinessException("分段规则之间存在缺口或重叠");
            }
        }
    }

    private String buildSummary(Map<String, Object> params, SystemConfig config) {
        List<String> changes = new ArrayList<>();
        if (params.containsKey("shopLat") || params.containsKey("shopLng")) changes.add("修改门店位置");
        if (params.containsKey("deliveryRadiusKm")) changes.add("修改配送半径");
        if (params.containsKey("deliveryMinAmount")) changes.add("修改起送金额");
        if (params.containsKey("deliveryFeeType")) changes.add("修改配送费类型");
        if (params.containsKey("fixedDeliveryFee")) changes.add("修改固定配送费");
        if (params.containsKey("tieredDeliveryFeeRules")) changes.add("修改分段配送规则");
        if (params.containsKey("orderTimeEnabled") || params.containsKey("orderStartTime") || params.containsKey("orderEndTime"))
            changes.add("修改接单时间");
        if (params.containsKey("qywxWebhookUrl")) changes.add("修改企业微信通知");
        if (params.containsKey("paymentQrUrl")) changes.add("修改收款二维码");
        if (changes.isEmpty()) changes.add("更新系统配置");
        return String.join("、", changes);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    private java.time.LocalTime parseTime(Object val) {
        if (val == null || val.toString().isEmpty()) return null;
        return java.time.LocalTime.parse(val.toString());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
