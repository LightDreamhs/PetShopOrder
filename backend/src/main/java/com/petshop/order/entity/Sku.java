package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Sku extends BaseEntity {

    private Long productId;
    private String specName;
    private BigDecimal price;
    private BigDecimal memberPrice;
    private Integer stock;
    private Integer sort;
}
