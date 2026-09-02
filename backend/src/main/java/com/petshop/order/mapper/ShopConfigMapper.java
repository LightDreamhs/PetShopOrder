package com.petshop.order.mapper;

import com.petshop.order.entity.ShopConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopConfigMapper {

    ShopConfig selectByShopId(Long shopId);

    int updateById(ShopConfig config);

    int insert(ShopConfig config);
}
