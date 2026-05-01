package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class MemberLevel extends BaseEntity {

    private String name;
    private BigDecimal discountRate;
    private Integer sort;
    private Integer status;
    private Integer memberCount;
}
