package com.petshop.order.mapper;

import com.petshop.order.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AppUserMapper {

    AppUser selectByPhone(@Param("phone") String phone);

    AppUser selectById(@Param("id") Long id);

    int insert(AppUser appUser);

    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);
}
