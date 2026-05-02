package com.petshop.order.service;

import com.petshop.order.common.PageResult;

import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(Map<String, Object> req);

    PageResult<Map<String, Object>> getMyOrders(Long userId, int page, int size);

    Map<String, Object> getOrderDetail(Long orderId, Long userId);

    PageResult<Map<String, Object>> getAdminOrders(int page, int size, String keyword,
                                                    Integer processed, Integer needDelivery,
                                                    String startTime, String endTime);

    Map<String, Object> getAdminOrderDetail(Long orderId);

    void updateProcessed(Long orderId, boolean processed);
}
