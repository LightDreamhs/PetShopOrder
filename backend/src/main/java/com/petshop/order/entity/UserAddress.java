package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAddress extends BaseEntity {

    private Long userId;
    private String label;
    private String address;
    private String detail;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer isDefault;
}
