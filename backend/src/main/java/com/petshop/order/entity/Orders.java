package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Orders extends BaseEntity {

    private String orderNo;
    private Long userId;
    private String customerPhoneSnapshot;
    private String customerName;
    private Long memberId;
    private String memberLevelSnapshot;
    private BigDecimal goodsAmount;
    private BigDecimal serviceAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private Integer needDelivery;
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private Integer deliveryDistance;
    private Integer processed;
    private String remark;
}
