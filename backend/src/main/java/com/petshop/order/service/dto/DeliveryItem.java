package com.petshop.order.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryItem {

    private Long productId;
    private Long skuId;
    private Integer quantity;
    private String type;
    private BigDecimal originalPrice;
    private Boolean supportDelivery;
}
