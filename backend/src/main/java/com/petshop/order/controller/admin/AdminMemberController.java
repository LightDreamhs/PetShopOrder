package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.Member;
import com.petshop.order.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MemberService memberService;

    private void checkManager() {
        if (!StpUtil.hasRole("BOSS") && !StpUtil.hasRole("MANAGER")) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    @GetMapping
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long levelId) {
        checkManager();
        PageResult<Member> pr = memberService.getList(page, size, keyword, levelId);
        List<Map<String, Object>> list = pr.getList().stream().map(this::toListMap).toList();
        Map<String, Object> result = Map.of(
                "list", list,
                "total", pr.getTotal(),
                "page", pr.getPage(),
                "size", pr.getSize()
        );
        return R.ok(result);
    }

    @PostMapping
    public R<Map<String, Object>> create(@Valid @RequestBody MemberRequest req) {
        checkManager();
        Member member = new Member();
        member.setName(req.getName());
        member.setLevelId(req.getLevelId());
        member.setRemark(req.getRemark());
        member.setPhones(req.getPhones());
        Member created = memberService.create(member);
        return R.ok(toDetailMap(created));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        checkManager();
        Member member = new Member();
        member.setName(req.getName());
        member.setLevelId(req.getLevelId());
        member.setRemark(req.getRemark());
        member.setPhones(req.getPhones());
        Member updated = memberService.update(id, member);
        return R.ok(toDetailMap(updated));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        checkManager();
        memberService.delete(id);
        return R.ok();
    }

    private Map<String, Object> toListMap(Member m) {
        return Map.of(
                "id", m.getId(),
                "name", m.getName(),
                "phones", m.getPhones() != null ? m.getPhones() : List.of(),
                "levelId", m.getLevelId(),
                "levelName", m.getLevelName() != null ? m.getLevelName() : "",
                "remark", m.getRemark() != null ? m.getRemark() : "",
                "createTime", m.getCreateTime() != null ? m.getCreateTime().format(FMT) : ""
        );
    }

    private Map<String, Object> toDetailMap(Member m) {
        return Map.of(
                "id", m.getId(),
                "name", m.getName(),
                "phones", m.getPhones() != null ? m.getPhones() : List.of(),
                "levelId", m.getLevelId(),
                "levelName", m.getLevelName() != null ? m.getLevelName() : "",
                "remark", m.getRemark() != null ? m.getRemark() : "",
                "createTime", m.getCreateTime() != null ? m.getCreateTime().format(FMT) : ""
        );
    }

    @Data
    public static class MemberRequest {
        @NotBlank(message = "会员名称不能为空")
        private String name;
        @NotNull(message = "会员等级不能为空")
        private Long levelId;
        private String remark;
        private List<String> phones;
    }
}
