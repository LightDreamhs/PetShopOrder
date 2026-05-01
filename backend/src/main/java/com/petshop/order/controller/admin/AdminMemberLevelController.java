package com.petshop.order.controller.admin;

import com.petshop.order.common.R;
import com.petshop.order.entity.MemberLevel;
import com.petshop.order.service.MemberLevelService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/member-levels")
@RequiredArgsConstructor
public class AdminMemberLevelController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MemberLevelService memberLevelService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        List<MemberLevel> list = memberLevelService.getList();
        return R.ok(list.stream().map(this::toMap).toList());
    }

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody MemberLevelRequest req) {
        MemberLevel level = new MemberLevel();
        level.setName(req.getName());
        level.setDiscountRate(req.getDiscountRate());
        level.setSort(req.getSort() != null ? req.getSort() : 0);
        MemberLevel created = memberLevelService.create(level);
        return R.ok(toMap(created));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody MemberLevelRequest req) {
        MemberLevel level = new MemberLevel();
        level.setName(req.getName());
        level.setDiscountRate(req.getDiscountRate());
        level.setSort(req.getSort());
        MemberLevel updated = memberLevelService.update(id, level);
        return R.ok(toMap(updated));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Integer dbStatus = "ENABLED".equals(status) ? 1 : 0;
        memberLevelService.updateStatus(id, dbStatus);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        memberLevelService.delete(id);
        return R.ok();
    }

    private Map<String, Object> toMap(MemberLevel l) {
        return Map.of(
                "id", l.getId(),
                "name", l.getName(),
                "discountRate", l.getDiscountRate().toPlainString(),
                "sort", l.getSort(),
                "status", l.getStatus() != null && l.getStatus() == 1 ? "ENABLED" : "DISABLED",
                "memberCount", l.getMemberCount() != null ? l.getMemberCount() : 0,
                "createTime", l.getCreateTime() != null ? l.getCreateTime().format(FMT) : ""
        );
    }

    @Data
    public static class MemberLevelRequest {
        @NotBlank
        private String name;
        @NotNull
        private BigDecimal discountRate;
        private Integer sort;
    }
}
