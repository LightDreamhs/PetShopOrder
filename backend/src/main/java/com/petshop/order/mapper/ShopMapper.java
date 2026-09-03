package com.petshop.order.mapper;

import com.petshop.order.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShopMapper {

    Shop selectById(Long id);

    Shop selectByCode(String code);

    /** 默认店：sort 最小、id 最小的门店 */
    Shop selectDefault();

    List<Shop> selectAll();

    int insert(Shop shop);

    int updateById(Shop shop);

    int updateStatus(@org.apache.ibatis.annotations.Param("id") Long id,
                     @org.apache.ibatis.annotations.Param("status") String status);
}
