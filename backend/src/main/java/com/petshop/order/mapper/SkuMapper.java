package com.petshop.order.mapper;

import com.petshop.order.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkuMapper {

    List<Sku> selectByProductId(@Param("productId") Long productId);

    Sku selectById(@Param("id") Long id);

    int insert(Sku sku);

    int insertBatch(@Param("list") List<Sku> list);

    int deleteByProductId(@Param("productId") Long productId);
}
