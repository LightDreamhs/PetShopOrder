package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

    private Long orderId;
    /** 归属门店（冗余，便于按店统计） */
    private Long shopId;
    private Long productId;
    private Long skuId;
    private String type;
    private String productName;
    private String skuName;
    private BigDecimal originalPrice;
    private BigDecimal dealPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
