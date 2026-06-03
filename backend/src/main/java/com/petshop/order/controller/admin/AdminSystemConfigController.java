package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.R;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.service.AdminAuthService;
import com.petshop.order.service.SystemConfigService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;
    private final AdminAuthService adminAuthService;

    @GetMapping
    public R<Map<String, Object>> getConfig() {
        StpUtil.checkRole("BOSS");
        return R.ok(systemConfigService.getConfig());
    }

    @PutMapping
    public R<Map<String, Object>> updateConfig(@RequestBody UpdateConfigRequest req) {
        StpUtil.checkRole("BOSS");
        AdminUser operator = adminAuthService.getCurrentAdmin();
        Map<String, Object> params = buildUpdateParams(req);
        return R.ok(systemConfigService.updateConfig(params, operator));
    }

    @PostMapping("/test-webhook")
    public R<Map<String, Object>> testWebhook(@RequestBody TestWebhookRequest req) {
        StpUtil.checkRole("BOSS");
        return R.ok(systemConfigService.testWebhook(req.getWebhookUrl()));
    }

    private Map<String, Object> buildUpdateParams(UpdateConfigRequest req) {
        Map<String, Object> params = new HashMap<>();
        if (req.getShopLat() != null) params.put("shopLat", req.getShopLat());
        if (req.getShopLng() != null) params.put("shopLng", req.getShopLng());
        if (req.getDeliveryRadiusKm() != null) params.put("deliveryRadiusKm", req.getDeliveryRadiusKm());
        if (req.getDeliveryMinAmount() != null) params.put("deliveryMinAmount", req.getDeliveryMinAmount());
        if (req.getDeliveryFeeType() != null) params.put("deliveryFeeType", req.getDeliveryFeeType());
        if (req.getFixedDeliveryFee() != null) params.put("fixedDeliveryFee", req.getFixedDeliveryFee());
        if (req.getOrderTimeEnabled() != null) params.put("orderTimeEnabled", req.getOrderTimeEnabled());
        if (req.getOrderStartTime() != null) params.put("orderStartTime", req.getOrderStartTime());
        if (req.getOrderEndTime() != null) params.put("orderEndTime", req.getOrderEndTime());

        if (req.getQywxWebhookUrl() != null) {
            params.put("qywxWebhookUrl", req.getQywxWebhookUrl());
        }

        if (req.getPaymentQrUrl() != null) {
            params.put("paymentQrUrl", req.getPaymentQrUrl());
        }

        if (req.getTieredDeliveryFeeRules() != null) {
            List<Map<String, Object>> rules = new ArrayList<>();
            for (TierRule tierRule : req.getTieredDeliveryFeeRules()) {
                Map<String, Object> rule = new HashMap<>();
                rule.put("minDistanceKm", tierRule.getMinDistanceKm());
                rule.put("maxDistanceKm", tierRule.getMaxDistanceKm());
                rule.put("fee", tierRule.getFee());
                rules.add(rule);
            }
            params.put("tieredDeliveryFeeRules", rules);
        }

        return params;
    }

    @Data
    public static class UpdateConfigRequest {
        private BigDecimal shopLat;
        private BigDecimal shopLng;
        private BigDecimal deliveryRadiusKm;
        private BigDecimal deliveryMinAmount;
        private String deliveryFeeType;
        private BigDecimal fixedDeliveryFee;
        private List<TierRule> tieredDeliveryFeeRules;
        private Boolean orderTimeEnabled;
        private String orderStartTime;
        private String orderEndTime;
        private String qywxWebhookUrl;
        private String paymentQrUrl;
    }

    @Data
    public static class TierRule {
        private BigDecimal minDistanceKm;
        private BigDecimal maxDistanceKm;
        private BigDecimal fee;
    }

    @Data
    public static class TestWebhookRequest {
        private String webhookUrl;
    }
}
