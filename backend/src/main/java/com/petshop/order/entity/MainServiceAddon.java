package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MainServiceAddon extends BaseEntity {

    private Long mainProductId;
    private Long addonProductId;
    private Integer sort;
}
