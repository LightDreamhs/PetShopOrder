package com.petshop.order.service.dto;

import lombok.Data;

@Data
public class CalculatedItemResult {

    private Long productId;
    private Long skuId;
    private String productName;
    private String skuName;
    private String type;
    private String originalPrice;
    private String dealPrice;
    private Integer quantity;
    private String subtotal;
}
