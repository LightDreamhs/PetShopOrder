package com.petshop.order.mapper;

import com.petshop.order.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrdersMapper {

    int insert(Orders orders);

    Orders selectById(@Param("id") Long id);

    List<Orders> selectPageListByUserId(@Param("userId") Long userId);

    List<Orders> selectPageListAdmin(@Param("keyword") String keyword,
                                     @Param("processed") Integer processed,
                                     @Param("needDelivery") Integer needDelivery,
                                     @Param("startTime") String startTime,
                                     @Param("endTime") String endTime);

    int updateProcessed(@Param("id") Long id, @Param("processed") Integer processed);

    int updateCancelled(@Param("id") Long id, @Param("cancelled") Integer cancelled);

    int countNewOrders(@Param("since") String since);
}
