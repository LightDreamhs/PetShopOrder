package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.entity.Appointment;
import com.petshop.order.entity.AppUser;
import com.petshop.order.entity.Orders;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.entity.SystemConfig;
import com.petshop.order.mapper.AppointmentMapper;
import com.petshop.order.mapper.MainServiceAddonMapper;
import com.petshop.order.mapper.OrderItemMapper;
import com.petshop.order.mapper.OrdersMapper;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.mapper.SystemConfigMapper;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.AppointmentService;
import com.petshop.order.service.NotificationService;
import com.petshop.order.service.OrderService;
import com.petshop.order.service.dto.AppointmentNotifyInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final MainServiceAddonMapper mainServiceAddonMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final AppAuthService appAuthService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    /** 全局容量：任意时刻同时进行的服务 ≤ 此值（含自身） */
    private static final int MAX_CONCURRENT = 3;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ============================== 附加服务列表 ==============================

    @Override
    public List<Map<String, Object>> getAddons(Long mainProductId) {
        if (mainProductId == null) {
            throw new BusinessException("主服务 ID 不能为空");
        }
        Product main = productMapper.selectById(mainProductId);
        if (main == null || !"SERVICE".equals(main.getType())) {
            throw new BusinessException("主服务不存在");
        }
        return mainServiceAddonMapper.selectAddonsByMainProduct(mainProductId);
    }

    // ============================== 冲突预检 ==============================

    @Override
    public Map<String, Object> previewConflict(LocalDateTime startTime, Long mainSkuId, List<Long> addonSkuIds) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (startTime == null || mainSkuId == null) {
            result.put("available", false);
            result.put("message", "缺少开始时间或主服务规格");
            return result;
        }

        int totalDuration = calcTotalDuration(mainSkuId, addonSkuIds);
        LocalDateTime endTime = startTime.plusMinutes(totalDuration);
        int overlap = appointmentMapper.countOverlap(startTime, endTime);

        result.put("available", overlap < MAX_CONCURRENT);
        result.put("overlapCount", overlap);
        result.put("maxConcurrent", MAX_CONCURRENT);
        result.put("totalDuration", totalDuration);
        result.put("startTime", startTime.format(FMT));
        result.put("endTime", endTime.format(FMT));
        if (overlap >= MAX_CONCURRENT) {
            result.put("message", "该时段已约满，请选择其他时间");
        } else {
            result.put("message", "可预约");
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAvailableSlots(String date, Long mainSkuId, List<Long> addonSkuIds) {
        if (date == null || date.isEmpty() || mainSkuId == null) {
            throw new BusinessException("缺少日期或主服务规格");
        }
        java.time.LocalDate d;
        try {
            d = java.time.LocalDate.parse(date);
        } catch (Exception e) {
            throw new BusinessException("日期格式错误，应为 yyyy-MM-dd");
        }

        int totalDuration = calcTotalDuration(mainSkuId, addonSkuIds);

        // 营业时段（默认 09:00-21:00 开区间）
        SystemConfig cfg = systemConfigMapper.selectById(1L);
        LocalTime bStart = LocalTime.of(9, 0);
        LocalTime bEnd = LocalTime.of(21, 0);
        if (cfg != null && cfg.getOrderTimeEnabled() != null && cfg.getOrderTimeEnabled() == 1
                && cfg.getOrderStartTime() != null && cfg.getOrderEndTime() != null) {
            bStart = cfg.getOrderStartTime();
            bEnd = cfg.getOrderEndTime();
        }

        // 一次查出当天（含溢出缓冲）所有非取消预约区间
        LocalDateTime rangeStart = d.atTime(bStart);
        LocalDateTime rangeEnd = d.atTime(bEnd).plusMinutes(totalDuration);
        List<Appointment> active = appointmentMapper.selectActiveInRange(rangeStart, rangeEnd);

        List<Map<String, Object>> slots = new ArrayList<>();
        // 半小时步进生成候选起点，最后一个起点必须满足 start + totalDuration <= 营业结束
        LocalTime t = bStart;
        while (t.isBefore(bEnd)) {
            LocalDateTime slotStart = d.atTime(t);
            LocalDateTime slotEnd = slotStart.plusMinutes(totalDuration);
            // 结束时间不能超过营业结束（按开始时间在营业内的约束，结束可超？实际应限制结束 <= bEnd 才合理）
            boolean available = slotEnd.toLocalTime().isBefore(bEnd) || slotEnd.toLocalTime().equals(bEnd);
            // 起点已过去的时段不可约（今天）
            if (available && slotStart.isBefore(java.time.LocalDateTime.now())) {
                available = false;
            }
            // 区间重叠计数：与已有非取消预约重叠数 < MAX_CONCURRENT
            if (available) {
                int overlap = 0;
                for (Appointment a : active) {
                    if (a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart)) {
                        overlap++;
                    }
                }
                available = overlap < MAX_CONCURRENT;
            }

            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("time", String.format("%02d:%02d", t.getHour(), t.getMinute()));
            slot.put("available", available);
            slots.add(slot);

            t = t.plusMinutes(30);
        }
        return slots;
    }

    // ============================== 创建预约 ==============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAppointment(Map<String, Object> req) {
        // 1. 解析参数
        Long mainProductId = toLong(req.get("mainProductId"));
        Long mainSkuId = toLong(req.get("mainSkuId"));
        @SuppressWarnings("unchecked")
        List<Object> addonSkuIdRaws = (List<Object>) req.get("addonSkuIds");
        String startTimeStr = (String) req.get("startTime");
        String petInfo = (String) req.get("petInfo");
        String customerName = (String) req.get("customerName");
        String remark = (String) req.get("remark");

        if (mainProductId == null || mainSkuId == null) {
            throw new BusinessException("请选择主服务规格");
        }
        if (startTimeStr == null || startTimeStr.isEmpty()) {
            throw new BusinessException("请选择预约时间");
        }

        List<Long> addonSkuIds = new ArrayList<>();
        if (addonSkuIdRaws != null) {
            for (Object o : addonSkuIdRaws) {
                addonSkuIds.add(toLong(o));
            }
        }

        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(startTimeStr, FMT);
        } catch (Exception e) {
            throw new BusinessException("预约时间格式错误，应为 yyyy-MM-dd HH:mm");
        }

        // 2. 校验主服务是 MAIN_SERVICE
        Product mainProduct = productMapper.selectById(mainProductId);
        if (mainProduct == null || !"SERVICE".equals(mainProduct.getType())) {
            throw new BusinessException("主服务不存在");
        }
        if (!"MAIN_SERVICE".equals(mainProduct.getServiceCategory())) {
            throw new BusinessException("该商品不是主服务，无法预约");
        }

        // 3. 算总时长
        int totalDuration = calcTotalDuration(mainSkuId, addonSkuIds);
        if (totalDuration <= 0) {
            throw new BusinessException("服务时长无效，请联系店主配置");
        }
        LocalDateTime endTime = startTime.plusMinutes(totalDuration);

        // 4. 营业时间校验（仅约束预约开始时间）
        checkBusinessHours(startTime);

        // 5. 冲突校验
        int overlap = appointmentMapper.countOverlap(startTime, endTime);
        if (overlap >= MAX_CONCURRENT) {
            throw new BusinessException("该时段已约满，请选择其他时间");
        }

        // 6. 复用 OrderService.createOrder 生成订单（needDelivery=false，服务到店）
        AppUser currentUser = appAuthService.getCurrentUser();

        List<Map<String, Object>> items = new ArrayList<>();
        // 主服务
        Map<String, Object> mainItem = new LinkedHashMap<>();
        mainItem.put("productId", mainProductId);
        mainItem.put("skuId", mainSkuId);
        mainItem.put("quantity", 1);
        items.add(mainItem);
        // 附加服务
        for (Long addonSkuId : addonSkuIds) {
            Long addonProductId = resolveProductIdBySku(addonSkuId);
            Map<String, Object> addonItem = new LinkedHashMap<>();
            addonItem.put("productId", addonProductId);
            addonItem.put("skuId", addonSkuId);
            addonItem.put("quantity", 1);
            items.add(addonItem);
        }

        Map<String, Object> orderReq = new LinkedHashMap<>();
        orderReq.put("items", items);
        orderReq.put("customerName", customerName);
        orderReq.put("needDelivery", false);
        // 预约摘要 + 用户备注拼进 remark，订单详情/Admin/通知都能看到
        StringBuilder remarkSb = new StringBuilder();
        remarkSb.append("预约 ").append(startTime.format(FMT))
                .append("（").append(totalDuration).append("分钟）");
        if (petInfo != null && !petInfo.isEmpty()) {
            remarkSb.append("；宠物：").append(petInfo);
        }
        if (remark != null && !remark.isEmpty()) {
            remarkSb.append("；备注：").append(remark);
        }
        orderReq.put("remark", remarkSb.toString());

        Map<String, Object> orderResult = orderService.createOrder(orderReq, true);
        Long orderId = toLong(orderResult.get("id"));
        if (orderId == null) {
            // 兜底：通过 orderNo 反查（理论上 createOrder 已带出 id）
            throw new BusinessException("订单创建失败");
        }

        // 7. 插入预约记录
        Appointment appt = new Appointment();
        appt.setOrderId(orderId);
        appt.setUserId(currentUser.getId());
        appt.setMainProductId(mainProductId);
        appt.setMainSkuId(mainSkuId);
        appt.setStartTime(startTime);
        appt.setEndTime(endTime);
        appt.setTotalDuration(totalDuration);
        appt.setPetInfo(petInfo);
        appt.setStatus("PENDING");
        appointmentMapper.insert(appt);

        // 8. 发送预约通知（带预约时间/总时长/宠物信息，替代普通订单通知）
        try {
            Orders order = ordersMapper.selectById(orderId);
            List<com.petshop.order.entity.OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
            AppointmentNotifyInfo notifyInfo = new AppointmentNotifyInfo();
            notifyInfo.setStartTime(startTime);
            notifyInfo.setEndTime(endTime);
            notifyInfo.setTotalDuration(totalDuration);
            notifyInfo.setPetInfo(petInfo);
            notificationService.sendNewOrderNotification(order, orderItems, notifyInfo);
        } catch (Exception e) {
            log.warn("发送预约通知失败，orderId={}", orderId, e);
        }

        // 9. 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointmentId", appt.getId());
        result.put("orderId", orderId);
        result.put("orderNo", orderResult.get("orderNo"));
        result.put("startTime", startTime.format(FMT));
        result.put("endTime", endTime.format(FMT));
        result.put("totalDuration", totalDuration);
        result.put("totalAmount", orderResult.get("totalAmount"));
        return result;
    }

    // ============================== 取消预约 ==============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long appointmentId, Long userId) {
        if (appointmentId == null || userId == null) {
            throw new BusinessException("参数缺失");
        }
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException("预约不存在");
        }
        if (!appt.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该预约");
        }
        if ("CANCELLED".equals(appt.getStatus())) {
            throw new BusinessException("该预约已取消");
        }
        if ("SERVICED".equals(appt.getStatus())) {
            throw new BusinessException("该预约已完成服务，无法取消");
        }

        // 预约标记取消（时段随之释放，冲突检测排除 CANCELLED）
        appointmentMapper.updateStatus(appointmentId, "CANCELLED");
        // 订单联动标记取消
        ordersMapper.updateCancelled(appt.getOrderId(), 1);
    }

    // ============================== 我的预约 ==============================

    @Override
    public PageResult<Map<String, Object>> myAppointments(Long userId, String status, int page, int size) {
        PageHelper.startPage(page, size);
        List<Map<String, Object>> list = appointmentMapper.selectPageListByUserId(userId, status);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    // ============================== Admin 看板 ==============================

    @Override
    public PageResult<Map<String, Object>> adminList(String date, String status, String keyword, int page, int size) {
        // date（yyyy-MM-dd）→ 当天 [00:00:00, 次日 00:00:00) 区间，按预约开始时间过滤
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (date != null && !date.isEmpty()) {
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(date);
                startTime = d.atStartOfDay();
                endTime = d.plusDays(1).atStartOfDay();
            } catch (Exception e) {
                throw new BusinessException("日期格式错误，应为 yyyy-MM-dd");
            }
        }
        PageHelper.startPage(page, size);
        List<Map<String, Object>> list = appointmentMapper.selectPageListForAdmin(startTime, endTime, status, keyword);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminMarkServiced(Long appointmentId) {
        if (appointmentId == null) {
            throw new BusinessException("参数缺失");
        }
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException("预约不存在");
        }
        if (!"PENDING".equals(appt.getStatus())) {
            throw new BusinessException("仅待服务状态的预约可标记完成");
        }
        appointmentMapper.updateStatus(appointmentId, "SERVICED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long appointmentId) {
        if (appointmentId == null) {
            throw new BusinessException("参数缺失");
        }
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException("预约不存在");
        }
        if (!"PENDING".equals(appt.getStatus())) {
            throw new BusinessException("仅待服务状态的预约可取消");
        }
        appointmentMapper.updateStatus(appointmentId, "CANCELLED");
        ordersMapper.updateCancelled(appt.getOrderId(), 1);
    }

    // ============================== 内部工具 ==============================

    /**
     * 计算总占用时长（分钟），用于冲突检测。
     * - 主服务：duration 必填且 > 0，决定整个预约的占用时段。
     * - 附加服务：duration 缺省 0（不占时间，只加钱）；仅 duration > 0 的附加项才额外累加时长。
     */
    private int calcTotalDuration(Long mainSkuId, List<Long> addonSkuIds) {
        Sku mainSku = skuMapper.selectById(mainSkuId);
        if (mainSku == null || mainSku.getDuration() == null || mainSku.getDuration() <= 0) {
            throw new BusinessException("主服务未配置时长，请联系店主");
        }
        int total = mainSku.getDuration();
        if (addonSkuIds != null) {
            for (Long addonSkuId : addonSkuIds) {
                Sku addonSku = skuMapper.selectById(addonSkuId);
                if (addonSku == null) {
                    throw new BusinessException("附加服务规格不存在");
                }
                // duration <= 0 或 null：不占时间，跳过
                if (addonSku.getDuration() != null && addonSku.getDuration() > 0) {
                    total += addonSku.getDuration();
                }
            }
        }
        return total;
    }

    /** 营业时间校验：仅约束预约开始时间落在 [start, end) 内 */
    private void checkBusinessHours(LocalDateTime startTime) {
        SystemConfig cfg = systemConfigMapper.selectById(1L);
        if (cfg == null || cfg.getOrderTimeEnabled() == null || cfg.getOrderTimeEnabled() != 1) {
            return;
        }
        LocalTime start = cfg.getOrderStartTime();
        LocalTime end = cfg.getOrderEndTime();
        if (start == null || end == null) {
            return;
        }
        LocalTime t = startTime.toLocalTime();
        // 开始时间必须在 [start, end) 内
        if (t.isBefore(start) || !t.isBefore(end)) {
            throw new BusinessException("请在营业时间内预约（" + start + "-" + end + "）");
        }
    }

    /** 通过 skuId 反查 productId（构造订单 items 用） */
    private Long resolveProductIdBySku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("附加服务规格不存在: " + skuId);
        }
        return sku.getProductId();
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
