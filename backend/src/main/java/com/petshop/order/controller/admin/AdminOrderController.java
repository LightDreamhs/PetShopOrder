package com.petshop.order.controller.admin;

import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public R<PageResult<Map<String, Object>>> getAdminOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) Boolean needDelivery,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Integer processedInt = processed != null ? (processed ? 1 : 0) : null;
        Integer needDeliveryInt = needDelivery != null ? (needDelivery ? 1 : 0) : null;
        return R.ok(orderService.getAdminOrders(page, size, keyword, processedInt, needDeliveryInt, startTime, endTime));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getAdminOrderDetail(@PathVariable Long id) {
        return R.ok(orderService.getAdminOrderDetail(id));
    }

    @PutMapping("/{id}/processed")
    public R<Void> updateProcessed(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean processed = (Boolean) body.get("processed");
        orderService.updateProcessed(id, processed != null && processed);
        return R.ok();
    }

    @GetMapping("/new-count")
    public R<Map<String, Object>> getNewOrderCount(@RequestParam String since) {
        int count = orderService.getNewOrderCount(since);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", count);
        return R.ok(data);
    }
}
