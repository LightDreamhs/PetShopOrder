package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Shop extends BaseEntity {

    private String code;
    private String name;
    private String phone;
    private String address;
    private BigDecimal shopLat;
    private BigDecimal shopLng;
    /** OPEN 营业 / CLOSED 歇业 */
    private String status;
    private Integer sort;
}
