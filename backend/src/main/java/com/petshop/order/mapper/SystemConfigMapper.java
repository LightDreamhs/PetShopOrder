package com.petshop.order.mapper;

import com.petshop.order.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemConfigMapper {

    SystemConfig selectById(Long id);

    int updateById(SystemConfig config);

    int insert(SystemConfig config);
}
