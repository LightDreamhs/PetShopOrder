package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Appointment extends BaseEntity {

    private Long orderId;
    private Long userId;
    private Long mainProductId;
    private Long mainSkuId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalDuration;
    private String petInfo;
    private String status;
}
