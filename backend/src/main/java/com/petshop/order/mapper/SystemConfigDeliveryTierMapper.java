package com.petshop.order.mapper;

import com.petshop.order.entity.SystemConfigDeliveryTier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigDeliveryTierMapper {

    List<SystemConfigDeliveryTier> selectByConfigId(Long configId);

    int insertBatch(@Param("list") List<SystemConfigDeliveryTier> list);

    int deleteByConfigId(Long configId);
}
