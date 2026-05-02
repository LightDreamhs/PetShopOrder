package com.petshop.order.service;

import com.petshop.order.entity.AdminUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    Map<String, Object> getConfig();

    Map<String, Object> updateConfig(Map<String, Object> params, AdminUser operator);

    Map<String, Object> testWebhook(String webhookUrl);

    Map<String, Object> getShopLocation();

    Map<String, Object> getDeliveryConfig();

    List<Map<String, Object>> getTierRules(Long configId);
}
