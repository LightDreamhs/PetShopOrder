package com.petshop.order.mapper;

import com.petshop.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insertBatch(@Param("list") List<OrderItem> list);

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
