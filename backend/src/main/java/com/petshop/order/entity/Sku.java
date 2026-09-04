package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Sku extends BaseEntity {

    private Long productId;
    /** 归属门店（SKU 为店铺级价格/库存/上下架） */
    private Long shopId;
    private String specName;
    private BigDecimal price;
    private BigDecimal memberPrice;
    /** SKU 图片 URL（未配置时前端回退商品主图） */
    private String imgUrl;
    private Integer duration;
    private Integer stock;
    /** ON_SALE / OFF_SALE（店铺级上下架） */
    private String status;
    private Integer sort;
}
