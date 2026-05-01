package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.entity.Member;
import com.petshop.order.entity.MemberLevel;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.MemberLevelService;
import com.petshop.order.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/member")
@RequiredArgsConstructor
public class AppMemberController {

    private final AppAuthService appAuthService;
    private final MemberService memberService;
    private final MemberLevelService memberLevelService;

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        String phone = appAuthService.getCurrentUser().getPhone();
        Member member = memberService.getMemberByPhone(phone);

        Map<String, Object> result = new HashMap<>();
        if (member == null) {
            result.put("isMember", false);
            result.put("memberLevel", null);
            result.put("serviceDiscountText", null);
            return R.ok(result);
        }

        result.put("isMember", true);

        List<MemberLevel> levels = memberLevelService.getList();
        MemberLevel matchedLevel = levels.stream()
                .filter(l -> l.getId().equals(member.getLevelId()))
                .findFirst()
                .orElse(null);

        if (matchedLevel != null) {
            Map<String, Object> levelInfo = new HashMap<>();
            levelInfo.put("name", matchedLevel.getName());
            levelInfo.put("discountRate", matchedLevel.getDiscountRate().toString());
            result.put("memberLevel", levelInfo);
            result.put("serviceDiscountText", toDiscountText(matchedLevel.getDiscountRate()));
        } else {
            result.put("memberLevel", null);
            result.put("serviceDiscountText", null);
        }

        return R.ok(result);
    }

    private String toDiscountText(BigDecimal discountRate) {
        if (discountRate == null) {
            return null;
        }
        BigDecimal discount = discountRate.multiply(new BigDecimal("10"));
        String val = discount.stripTrailingZeros().toPlainString();
        return val + "折";
    }
}
