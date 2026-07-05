package com.petshop.order.service;

import com.petshop.order.common.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AppointmentService {

    /** 查某主服务绑定的可选附加服务（含默认 SKU 的价格/时长） */
    List<Map<String, Object>> getAddons(Long mainProductId);

    /**
     * 冲突预检：给定开始时间 + 主服务 + 附加服务，返回是否可约、重叠数、总时长。
     * 前端选时间时实时调用。不做营业时间硬校验（只返回信息）。
     */
    Map<String, Object> previewConflict(LocalDateTime startTime, Long mainSkuId, List<Long> addonSkuIds);

    /**
     * 时段可约状态：给定日期 + 主服务 + 附加服务（算总时长），返回该天营业时间内每个半小时时段的可约情况。
     * 用于预约页时间网格铺开展示（约满置灰）。
     */
    List<Map<String, Object>> getAvailableSlots(String date, Long mainSkuId, List<Long> addonSkuIds);

    /**
     * 创建预约：算总时长 → 营业时间校验 → 冲突校验 → 复用下单 → 插预约 → 预约通知。
     * req 字段：mainProductId, mainSkuId, addonSkuIds[], startTime, petInfo, customerName, userId(由 controller 注入)
     */
    Map<String, Object> createAppointment(Map<String, Object> req);

    /** 取消预约：校验归属 → appointment.status=CANCELLED → orders.cancelled=1 → 释放时段 */
    void cancel(Long appointmentId, Long userId);

    /** 我的预约分页列表（含订单快照信息） */
    PageResult<Map<String, Object>> myAppointments(Long userId, String status, int page, int size);

    // ============================== Admin 看板 ==============================

    /** Admin 看板：按日期（预约开始时间）+ 状态 + 关键词分页查询 */
    PageResult<Map<String, Object>> adminList(String date, String status, String keyword, int page, int size);

    /** Admin 标记预约完成（PENDING → SERVICED） */
    void adminMarkServiced(Long appointmentId);

    /** Admin 取消预约（→ CANCELLED，联动订单 cancelled=1，释放时段） */
    void adminCancel(Long appointmentId);
}
