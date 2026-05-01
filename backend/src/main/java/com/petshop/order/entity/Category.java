package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {

    private String name;
    private String icon;
    private String type;
    private Integer sort;

    private Integer productCount;
}
