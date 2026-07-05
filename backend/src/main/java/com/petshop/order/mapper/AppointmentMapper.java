package com.petshop.order.mapper;

import com.petshop.order.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AppointmentMapper {

    int insert(Appointment appointment);

    Appointment selectById(@Param("id") Long id);

    /** 按 order_id 查预约（含归属校验用） */
    Appointment selectByOrderId(@Param("orderId") Long orderId);

    /** 批量查多个订单的预约信息（用于订单列表补全），返回 Map 含 orderId 便于按订单分组 */
    List<Map<String, Object>> selectByOrderIds(@Param("orderIds") List<Long> orderIds);

    /** 单条订单的预约信息（用于订单详情补全），返回 Map */
    Map<String, Object> selectMapByOrderId(@Param("orderId") Long orderId);

    /** 冲突检测核心：数与新预约区间 [start,end) 重叠的、非取消的已有预约数 */
    int countOverlap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 我的预约分页列表（含订单快照信息） */
    List<Map<String, Object>> selectPageListByUserId(@Param("userId") Long userId,
                                                     @Param("status") String status);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** Admin 看板：按日期范围 + 状态 + 关键词（订单号/电话/联系人）分页查询 */
    List<Map<String, Object>> selectPageListForAdmin(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("status") String status,
                                                     @Param("keyword") String keyword);

    /** 查某时间区间内非取消的预约 [start,end) 列表（用于时段可约判断，只取区间字段） */
    List<Appointment> selectActiveInRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
