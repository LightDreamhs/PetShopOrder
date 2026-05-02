package com.petshop.order.mapper;

import com.petshop.order.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminUserMapper {

    AdminUser selectByUsername(@Param("username") String username);

    AdminUser selectById(@Param("id") Long id);

    int insert(AdminUser adminUser);

    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    List<AdminUser> selectAll();

    int updateById(AdminUser user);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int deleteById(@Param("id") Long id);
}
