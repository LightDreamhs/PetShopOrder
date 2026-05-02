package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.OperationLog;
import com.petshop.order.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OperationLogService operationLogService;

    @GetMapping
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        PageResult<OperationLog> pr = operationLogService.getList(page, size, userId, action, startTime, endTime);
        List<Map<String, Object>> list = pr.getList().stream().map(this::toMap).toList();
        Map<String, Object> result = Map.of(
                "list", list,
                "total", pr.getTotal(),
                "page", pr.getPage(),
                "size", pr.getSize()
        );
        return R.ok(result);
    }

    private Map<String, Object> toMap(OperationLog log) {
        return Map.of(
                "id", log.getId(),
                "userId", log.getUserId(),
                "username", log.getUsername() != null ? log.getUsername() : "",
                "action", log.getAction(),
                "target", log.getTarget() != null ? log.getTarget() : "",
                "before", log.getBeforeVal() != null ? log.getBeforeVal() : "",
                "after", log.getAfterVal() != null ? log.getAfterVal() : "",
                "time", log.getCreateTime() != null ? log.getCreateTime().format(FMT) : ""
        );
    }
}
