package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity {

    private String phone;
    private Integer status;
    private LocalDateTime lastLoginTime;
}
