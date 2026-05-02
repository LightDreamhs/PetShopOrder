package com.petshop.order.mapper;

import com.petshop.order.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    List<OperationLog> selectPageList(@Param("userId") Long userId,
                                      @Param("action") String action,
                                      @Param("startTime") String startTime,
                                      @Param("endTime") String endTime);

    int insert(OperationLog log);
}
