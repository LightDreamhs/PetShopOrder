package com.petshop.order.service.dto;

import lombok.Data;

@Data
public class CartItemInput {

    private Long productId;
    private Long skuId;
    private Integer quantity;
}
