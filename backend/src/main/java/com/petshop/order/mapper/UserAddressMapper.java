package com.petshop.order.mapper;

import com.petshop.order.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    List<UserAddress> selectByUserId(@Param("userId") Long userId);

    UserAddress selectById(@Param("id") Long id);

    int countByUserId(@Param("userId") Long userId);

    int insert(UserAddress userAddress);

    int updateById(UserAddress userAddress);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int setDefault(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}
