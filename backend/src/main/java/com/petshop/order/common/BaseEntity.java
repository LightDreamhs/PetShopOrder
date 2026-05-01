package com.petshop.order.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
