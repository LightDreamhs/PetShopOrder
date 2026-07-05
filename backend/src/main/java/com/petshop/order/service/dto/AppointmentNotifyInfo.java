package com.petshop.order.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约通知附加信息：预约下单后，商家通知在订单通知基础上追加展示。
 * 仅作为通知服务的传输载体，不映射数据库表。
 */
@Data
public class AppointmentNotifyInfo {

    /** 预约开始时间 */
    private LocalDateTime startTime;
    /** 预约结束时间 */
    private LocalDateTime endTime;
    /** 总占用时长（分钟） */
    private Integer totalDuration;
    /** 宠物信息（文本） */
    private String petInfo;
}
