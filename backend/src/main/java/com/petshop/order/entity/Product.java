package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

    private String name;
    private String description;
    private String coverImg;
    private String type;
    private String serviceCategory;
    private String status;
    private Integer supportDelivery;
    private Integer sort;
    private Integer skuCount;
    private BigDecimal minPrice;
    private List<Sku> skus;
}
