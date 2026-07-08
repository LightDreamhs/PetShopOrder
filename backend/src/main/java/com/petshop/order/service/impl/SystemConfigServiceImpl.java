package com.petshop.order.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
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
import java.security.MessageDigest;
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

    /**
     * 将任意长度的密钥字符串通过 SHA-256 派生为 32 字节的 AES-256 密钥
     */
    private byte[] deriveAesKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(aesKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("AES 密钥派生失败", e);
        }
    }

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
        List<SystemConfigDeliveryTier> oldTiers = tierMapper.selectByConfigId(CONFIG_ID);
        String beforeJson = toJson(config);
        List<String> changes = new ArrayList<>();

        // ===== 校验：集中前置，保证失败时不产生部分写入 =====
        if (params.containsKey("deliveryRadiusKm")) {
            BigDecimal radius = toBigDecimal(params.get("deliveryRadiusKm"));
            if (radius == null || radius.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("配送半径必须大于 0");
            }
        }
        if (params.containsKey("deliveryFeeType")) {
            String feeType = (String) params.get("deliveryFeeType");
            if (!"FREE".equals(feeType) && !"TIERED".equals(feeType)) {
                throw new BusinessException("配送费类型只支持 FREE 或 TIERED");
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tierRules = (List<Map<String, Object>>) params.get("tieredDeliveryFeeRules");
        BigDecimal effectiveRadius = params.containsKey("deliveryRadiusKm")
                ? toBigDecimal(params.get("deliveryRadiusKm"))
                : config.getDeliveryRadiusKm();
        String effectiveFeeType = params.containsKey("deliveryFeeType")
                ? (String) params.get("deliveryFeeType")
                : config.getDeliveryFeeType();
        if (tierRules != null && "TIERED".equals(effectiveFeeType)) {
            validateTierRules(tierRules, effectiveRadius);
        }

        // ===== 店铺位置（纬/经合并为一条）=====
        boolean shopChanged = false;
        BigDecimal newShopLat = config.getShopLat();
        BigDecimal newShopLng = config.getShopLng();
        if (params.containsKey("shopLat")) {
            newShopLat = toBigDecimal(params.get("shopLat"));
            if (!bdEquals(config.getShopLat(), newShopLat)) shopChanged = true;
        }
        if (params.containsKey("shopLng")) {
            newShopLng = toBigDecimal(params.get("shopLng"));
            if (!bdEquals(config.getShopLng(), newShopLng)) shopChanged = true;
        }
        if (shopChanged) {
            changes.add("修改店铺位置（" + coordLabel(config.getShopLat(), config.getShopLng())
                    + " → " + coordLabel(newShopLat, newShopLng) + "）");
            config.setShopLat(newShopLat);
            config.setShopLng(newShopLng);
        }

        // ===== 配送半径 =====
        if (params.containsKey("deliveryRadiusKm")) {
            BigDecimal radius = toBigDecimal(params.get("deliveryRadiusKm"));
            if (!bdEquals(config.getDeliveryRadiusKm(), radius)) {
                changes.add("修改配送半径（" + bdLabel(config.getDeliveryRadiusKm()) + " km → " + bdLabel(radius) + " km）");
                config.setDeliveryRadiusKm(radius);
            }
        }

        // ===== 起送价 =====
        if (params.containsKey("deliveryMinAmount")) {
            BigDecimal minAmount = toBigDecimal(params.get("deliveryMinAmount"));
            if (!bdEquals(config.getDeliveryMinAmount(), minAmount)) {
                changes.add("修改起送价（" + bdLabel(config.getDeliveryMinAmount()) + " 元 → " + bdLabel(minAmount) + " 元）");
                config.setDeliveryMinAmount(minAmount);
            }
        }

        // ===== 运费策略 =====
        if (params.containsKey("deliveryFeeType")) {
            String feeType = (String) params.get("deliveryFeeType");
            if (!feeType.equals(config.getDeliveryFeeType())) {
                changes.add("修改运费策略（" + feeTypeLabel(config.getDeliveryFeeType()) + " → " + feeTypeLabel(feeType) + "）");
                config.setDeliveryFeeType(feeType);
            }
        }

        // ===== 固定运费（保留处理逻辑以兼容历史，前端已无入口）=====
        if (params.containsKey("fixedDeliveryFee")) {
            BigDecimal fixedFee = toBigDecimal(params.get("fixedDeliveryFee"));
            if (!bdEquals(config.getFixedDeliveryFee(), fixedFee)) {
                changes.add("修改固定运费（" + bdLabel(config.getFixedDeliveryFee()) + " 元 → " + bdLabel(fixedFee) + " 元）");
                config.setFixedDeliveryFee(fixedFee);
            }
        }

        // ===== 预约时段（启用开关 + 起止合并为一条）=====
        Integer newOrderTimeEnabled = config.getOrderTimeEnabled();
        java.time.LocalTime newStartTime = config.getOrderStartTime();
        java.time.LocalTime newEndTime = config.getOrderEndTime();
        if (params.containsKey("orderTimeEnabled")) {
            newOrderTimeEnabled = toBoolean(params.get("orderTimeEnabled")) ? 1 : 0;
        }
        if (params.containsKey("orderStartTime")) {
            newStartTime = parseTime(params.get("orderStartTime"));
        }
        if (params.containsKey("orderEndTime")) {
            newEndTime = parseTime(params.get("orderEndTime"));
        }
        boolean timeChanged = !Objects.equals(config.getOrderTimeEnabled(), newOrderTimeEnabled)
                || !Objects.equals(config.getOrderStartTime(), newStartTime)
                || !Objects.equals(config.getOrderEndTime(), newEndTime);
        if (timeChanged) {
            changes.add("修改预约时段（"
                    + timeRangeLabel(config.getOrderTimeEnabled(), config.getOrderStartTime(), config.getOrderEndTime())
                    + " → "
                    + timeRangeLabel(newOrderTimeEnabled, newStartTime, newEndTime) + "）");
            config.setOrderTimeEnabled(newOrderTimeEnabled);
            config.setOrderStartTime(newStartTime);
            config.setOrderEndTime(newEndTime);
        }

        // ===== 群机器人 Webhook 通知 =====
        if (params.containsKey("qywxWebhookUrl")) {
            String webhookUrl = (String) params.get("qywxWebhookUrl");
            // 兼容飞书(hook/***)与企微(key=***)两种脱敏回传，回传脱敏值 = 不修改
            if (webhookUrl != null && !webhookUrl.contains("key=***") && !webhookUrl.contains("hook/***")) {
                String oldDecrypted = decryptWebhook(config);
                boolean oldHas = oldDecrypted != null && !oldDecrypted.isEmpty();
                boolean newHas = !webhookUrl.isEmpty();
                if (oldHas != newHas || (oldHas && !oldDecrypted.equals(webhookUrl))) {
                    if (!newHas) {
                        changes.add("清空群机器人通知");
                        config.setQywxWebhookUrlEnc(null);
                        config.setHasQywxWebhook(0);
                    } else {
                        validateWebhookUrl(webhookUrl);
                        AES aes = SecureUtil.aes(deriveAesKey());
                        config.setQywxWebhookUrlEnc(aes.encrypt(webhookUrl));
                        config.setHasQywxWebhook(1);
                        changes.add(oldHas ? "修改群机器人通知" : "设置群机器人通知");
                    }
                }
            }
        }

        // ===== 收款二维码 =====
        if (params.containsKey("paymentQrUrl")) {
            String newQr = (String) params.get("paymentQrUrl");
            String oldQr = config.getPaymentQrUrl();
            boolean oldHas = oldQr != null && !oldQr.isEmpty();
            boolean newHas = newQr != null && !newQr.isEmpty();
            if (oldHas != newHas || (oldHas && !oldQr.equals(newQr))) {
                if (!newHas) changes.add("移除收款二维码");
                else if (!oldHas) changes.add("设置收款二维码");
                else changes.add("更换收款二维码");
            }
            config.setPaymentQrUrl(newQr);
        }

        // ===== 开屏广告 =====
        if (params.containsKey("adEnabled")) {
            Integer newEn = Boolean.TRUE.equals(params.get("adEnabled")) ? 1 : 0;
            Integer oldEn = config.getAdEnabled();
            if (oldEn == null || !oldEn.equals(newEn)) {
                changes.add(newEn == 1 ? "开启开屏广告" : "关闭开屏广告");
            }
            config.setAdEnabled(newEn);
        }
        if (params.containsKey("adImageUrl")) {
            String newImg = (String) params.get("adImageUrl");
            String oldImg = config.getAdImageUrl();
            boolean oldHas = oldImg != null && !oldImg.isEmpty();
            boolean newHas = newImg != null && !newImg.isEmpty();
            if (oldHas != newHas || (oldHas && !oldImg.equals(newImg))) {
                if (!newHas) changes.add("移除开屏广告图");
                else if (!oldHas) changes.add("设置开屏广告图");
                else changes.add("更换开屏广告图");
            }
            config.setAdImageUrl(newImg);
        }
        if (params.containsKey("adLinkType")) {
            config.setAdLinkType((String) params.get("adLinkType"));
        }
        if (params.containsKey("adLinkTarget")) {
            config.setAdLinkTarget((String) params.get("adLinkTarget"));
        }

        config.setUpdatedBy(operator.getId());
        systemConfigMapper.updateById(config);

        // ===== 分段运费规则（独立表，段数与区间均参与 diff）=====
        if (tierRules != null && "TIERED".equals(config.getDeliveryFeeType())) {
            String oldSig = tierSignature(oldTiers);
            String newSig = tierSignatureFromParams(tierRules);
            if (!oldSig.equals(newSig)) {
                changes.add("修改分段运费规则（" + oldTiers.size() + " 段 → " + tierRules.size() + " 段）");
            }
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

        String summary = changes.isEmpty() ? "无变更" : String.join("；", changes);

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
            config.setAdEnabled(0);
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
                AES aes = SecureUtil.aes(deriveAesKey());
                String decrypted = aes.decryptStr(config.getQywxWebhookUrlEnc());
                maskedWebhook = maskWebhookKey(decrypted);
            } catch (Exception e) {
                maskedWebhook = null;
            }
        }
        m.put("qywxWebhookUrl", maskedWebhook);
        m.put("hasQywxWebhook", config.getHasQywxWebhook() != null && config.getHasQywxWebhook() == 1);
        m.put("paymentQrUrl", config.getPaymentQrUrl());
        m.put("adEnabled", config.getAdEnabled() != null && config.getAdEnabled() == 1);
        m.put("adImageUrl", config.getAdImageUrl());
        m.put("adLinkType", config.getAdLinkType());
        m.put("adLinkTarget", config.getAdLinkTarget());

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

    /** BigDecimal 安全等值比较（null 安全，用 compareTo 避免精度差异如 5.00 vs 5.0）*/
    private boolean bdEquals(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    /** BigDecimal 转可读字符串（null → "未设置"）*/
    private String bdLabel(BigDecimal v) {
        return v == null ? "未设置" : v.stripTrailingZeros().toPlainString();
    }

    private String coordLabel(BigDecimal lat, BigDecimal lng) {
        if (lat == null && lng == null) return "未设置";
        return bdLabel(lat) + ", " + bdLabel(lng);
    }

    private String feeTypeLabel(String feeType) {
        if ("FREE".equals(feeType)) return "免运费";
        if ("TIERED".equals(feeType)) return "分段运费";
        if ("FIXED".equals(feeType)) return "固定运费";
        return feeType == null ? "未设置" : feeType;
    }

    private String timeRangeLabel(Integer enabled, java.time.LocalTime start, java.time.LocalTime end) {
        if (enabled == null || enabled == 0) return "不限制";
        String s = start == null ? "?" : start.format(TIME_FMT);
        String e = end == null ? "?" : end.format(TIME_FMT);
        return s + " ~ " + e;
    }

    /** 分段规则的"归一签名"：按 min 排序后拼成 min~max=fee，用于检测是否真的变了 */
    private String tierSignature(List<SystemConfigDeliveryTier> tiers) {
        if (tiers == null || tiers.isEmpty()) return "";
        return tiers.stream()
                .sorted(Comparator.comparing(t -> t.getMinDistanceKm() == null ? BigDecimal.ZERO : t.getMinDistanceKm()))
                .map(t -> bdLabel(t.getMinDistanceKm()) + "~" + bdLabel(t.getMaxDistanceKm()) + "=" + bdLabel(t.getFee()))
                .collect(Collectors.joining("|"));
    }

    @SuppressWarnings("unchecked")
    private String tierSignatureFromParams(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return "";
        return rules.stream()
                .sorted(Comparator.comparing(r -> {
                    BigDecimal min = toBigDecimal(r.get("minDistanceKm"));
                    return min == null ? BigDecimal.ZERO : min;
                }))
                .map(r -> bdLabel(toBigDecimal(r.get("minDistanceKm"))) + "~"
                        + bdLabel(toBigDecimal(r.get("maxDistanceKm"))) + "="
                        + bdLabel(toBigDecimal(r.get("fee"))))
                .collect(Collectors.joining("|"));
    }

    /** 解密当前存储的 webhook 明文（用于 diff），失败/空返回 null */
    private String decryptWebhook(SystemConfig config) {
        if (config.getHasQywxWebhook() == null || config.getHasQywxWebhook() == 0
                || config.getQywxWebhookUrlEnc() == null) {
            return null;
        }
        try {
            AES aes = SecureUtil.aes(deriveAesKey());
            return aes.decryptStr(config.getQywxWebhookUrlEnc());
        } catch (Exception e) {
            return null;
        }
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
