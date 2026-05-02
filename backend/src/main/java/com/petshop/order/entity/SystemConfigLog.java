package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigLog extends BaseEntity {

    private Long configId;
    private Long operatorId;
    private String operatorName;
    private String summary;
    private String beforeVal;
    private String afterVal;
}
