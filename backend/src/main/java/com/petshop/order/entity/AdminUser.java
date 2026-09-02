package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUser extends BaseEntity {

    private String username;
    private String passwordHash;
    private String realName;
    private String role;
    /** 归属门店；NULL=总部（BOSS 可跨店） */
    private Long shopId;
    private Integer status;
    private LocalDateTime lastLoginTime;
}
