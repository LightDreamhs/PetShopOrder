package com.petshop.order.mapper;

import com.petshop.order.entity.SystemConfigLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigLogMapper {

    List<SystemConfigLog> selectRecentByConfigId(@Param("configId") Long configId, @Param("limit") int limit);

    int insert(SystemConfigLog log);
}
