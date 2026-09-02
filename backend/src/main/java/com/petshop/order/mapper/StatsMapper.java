package com.petshop.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    Map<String, Object> selectOverview(@Param("shopId") Long shopId);

    List<Map<String, Object>> selectOrderTrends(@Param("period") String period, @Param("shopId") Long shopId);

    List<Map<String, Object>> selectMemberRanking(@Param("limit") int limit, @Param("shopId") Long shopId);
}
