package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 店铺配置（每店一行，承接原 system_config 单行表；fixed_delivery_fee 已下线不再保留）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopConfig extends BaseEntity {

    private Long shopId;
    private BigDecimal shopLat;
    private BigDecimal shopLng;
    private BigDecimal deliveryRadiusKm;
    private BigDecimal deliveryMinAmount;
    private String deliveryFeeType;
    private Integer orderTimeEnabled;
    private LocalTime orderStartTime;
    private LocalTime orderEndTime;
    private byte[] qywxWebhookUrlEnc;
    private Integer hasQywxWebhook;
    private String paymentQrUrl;
    private Integer adEnabled;
    private String adImageUrl;
    private String adLinkType;
    private String adLinkTarget;
    private Long updatedBy;
}
