package com.petshop.order.service;

import com.petshop.order.entity.AdminUser;

import java.util.List;
import java.util.Map;

/**
 * 店铺配置服务（替代原 SystemConfigService；数据落在 shop_config / shop_delivery_tier，
 * 操作对象由 ShopContext 决定的当前门店）。
 */
public interface ShopConfigService {

    /** 当前店的配置 + 最近变更日志（变更日志仍存 system_config_log，config_id=shop_config.id） */
    Map<String, Object> getConfig();

    Map<String, Object> updateConfig(Map<String, Object> params, AdminUser operator);

    Map<String, Object> testWebhook(String webhookUrl);

    Map<String, Object> getShopLocation();

    Map<String, Object> getDeliveryConfig();

    List<Map<String, Object>> getTierRules(Long shopId);
}
