package com.petshop.order.mapper;

import com.petshop.order.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminUserMapper {

    AdminUser selectByUsername(@Param("username") String username);

    AdminUser selectById(@Param("id") Long id);

    int insert(AdminUser adminUser);

    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);
}
