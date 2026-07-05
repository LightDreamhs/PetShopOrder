package com.petshop.order.service;

import com.petshop.order.entity.OrderItem;
import com.petshop.order.entity.Orders;
import com.petshop.order.service.dto.AppointmentNotifyInfo;

import java.util.List;

public interface NotificationService {

    void sendNewOrderNotification(Orders order, List<OrderItem> items);

    /**
     * 预约下单通知：在普通订单通知基础上追加预约时间 / 总时长 / 宠物信息。
     * appointmentInfo 为 null 时退化为普通订单通知。
     */
    void sendNewOrderNotification(Orders order, List<OrderItem> items, AppointmentNotifyInfo appointmentInfo);
}
