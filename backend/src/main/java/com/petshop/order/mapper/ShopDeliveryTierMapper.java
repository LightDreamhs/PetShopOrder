package com.petshop.order.mapper;

import com.petshop.order.entity.ShopDeliveryTier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShopDeliveryTierMapper {

    List<ShopDeliveryTier> selectByShopId(Long shopId);

    int insertBatch(@Param("list") List<ShopDeliveryTier> list);

    int deleteByShopId(Long shopId);
}
