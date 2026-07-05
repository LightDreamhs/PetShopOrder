package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin 端预约管理：看板列表 + 标记完成 + 取消。
 * 列表对所有已登录 Admin 开放（参考 AdminOrderController 现状）；
 * 写操作（标记完成/取消）限 BOSS/MANAGER。
 */
@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    private void checkManager() {
        if (!StpUtil.hasRole("BOSS") && !StpUtil.hasRole("MANAGER")) {
            throw new BusinessException(403, "无权限操作");
        }
    }

    /** 看板列表：按日期（预约开始时间）+ 状态 + 关键词查询 */
    @GetMapping
    public R<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(appointmentService.adminList(date, status, keyword, page, size));
    }

    /** 标记预约完成（PENDING → SERVICED） */
    @PutMapping("/{id}/serviced")
    public R<Void> markServiced(@PathVariable Long id) {
        checkManager();
        appointmentService.adminMarkServiced(id);
        return R.ok();
    }

    /** 取消预约（→ CANCELLED，联动订单 cancelled=1） */
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        checkManager();
        appointmentService.adminCancel(id);
        return R.ok();
    }
}
