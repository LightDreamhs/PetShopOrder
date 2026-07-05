package com.petshop.order.controller.app;

import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.AppUser;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/appointments")
@RequiredArgsConstructor
public class AppAppointmentController {

    private final AppointmentService appointmentService;
    private final AppAuthService appAuthService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 主服务的附加服务列表 */
    @GetMapping("/addons")
    public R<List<Map<String, Object>>> addons(@RequestParam Long mainProductId) {
        return R.ok(appointmentService.getAddons(mainProductId));
    }

    /** 冲突预检（选时间时实时调用） */
    @PostMapping("/check")
    public R<Map<String, Object>> check(@RequestBody Map<String, Object> req) {
        LocalDateTime startTime = parseTime(req.get("startTime"));
        Long mainSkuId = toLong(req.get("mainSkuId"));
        List<Long> addonSkuIds = parseLongList(req.get("addonSkuIds"));
        return R.ok(appointmentService.previewConflict(startTime, mainSkuId, addonSkuIds));
    }

    /** 某日时段可约状态（时间网格铺开展示用） */
    @GetMapping("/slots")
    public R<List<Map<String, Object>>> slots(@RequestParam String date,
                                              @RequestParam Long mainSkuId,
                                              @RequestParam(required = false) List<Long> addonSkuIds) {
        return R.ok(appointmentService.getAvailableSlots(date, mainSkuId, addonSkuIds));
    }

    /** 创建预约（同事务生成订单+预约） */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> req) {
        AppUser currentUser = appAuthService.getCurrentUser();
        req.put("_userId", currentUser.getId());
        return R.ok(appointmentService.createAppointment(req));
    }

    /** 取消预约 */
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        AppUser currentUser = appAuthService.getCurrentUser();
        appointmentService.cancel(id, currentUser.getId());
        return R.ok();
    }

    /** 我的预约列表 */
    @GetMapping("/mine")
    public R<PageResult<Map<String, Object>>> mine(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String status) {
        AppUser currentUser = appAuthService.getCurrentUser();
        return R.ok(appointmentService.myAppointments(currentUser.getId(), status, page, size));
    }

    // ============================== 工具 ==============================

    private LocalDateTime parseTime(Object val) {
        if (val == null) return null;
        return LocalDateTime.parse(val.toString(), FMT);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseLongList(Object val) {
        if (val == null) return null;
        List<Long> result = new ArrayList<>();
        for (Object o : (List<Object>) val) {
            result.add(toLong(o));
        }
        return result;
    }
}
