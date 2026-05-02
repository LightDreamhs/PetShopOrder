package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends BaseEntity {

    private BigDecimal shopLat;
    private BigDecimal shopLng;
    private BigDecimal deliveryRadiusKm;
    private BigDecimal deliveryMinAmount;
    private String deliveryFeeType;
    private BigDecimal fixedDeliveryFee;
    private Integer orderTimeEnabled;
    private LocalTime orderStartTime;
    private LocalTime orderEndTime;
    private byte[] qywxWebhookUrlEnc;
    private Integer hasQywxWebhook;
    private Long updatedBy;
}
