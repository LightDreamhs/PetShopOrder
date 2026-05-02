package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigDeliveryTier extends BaseEntity {

    private Long configId;
    private BigDecimal minDistanceKm;
    private BigDecimal maxDistanceKm;
    private BigDecimal fee;
    private Integer sort;
}
