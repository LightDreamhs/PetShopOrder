package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseEntity {

    private Long userId;
    private String action;
    private String target;
    private String beforeVal;
    private String afterVal;

    private String username;
}
