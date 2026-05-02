package com.petshop.order.service;

import com.petshop.order.entity.OrderItem;
import com.petshop.order.entity.Orders;

import java.util.List;

public interface NotificationService {

    void sendNewOrderNotification(Orders order, List<OrderItem> items);
}
