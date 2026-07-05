package com.petshop.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.AppUser;
import com.petshop.order.mapper.AppUserMapper;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.sms.SmsVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private final AppUserMapper appUserMapper;
    private final SmsVerifyService smsVerifyService;

    @Override
    public void sendSmsCode(String phone) {
        // 验证码的生成/有效期/防刷/核验全部委托给 SmsVerifyService（log 兜底或 aliyun 真实）
        smsVerifyService.send(phone);
    }

    @Override
    public Map<String, Object> login(String phone, String code) {
        if (!smsVerifyService.verify(phone, code)) {
            throw new BusinessException("验证码错误");
        }

        AppUser user = appUserMapper.selectByPhone(phone);
        boolean isNew = false;
        if (user == null) {
            user = new AppUser();
            user.setPhone(phone);
            user.setStatus(1);
            user.setLastLoginTime(LocalDateTime.now());
            appUserMapper.insert(user);
            isNew = true;
        } else {
            if (user.getStatus() == 0) {
                throw new BusinessException("账号已被禁用");
            }
            appUserMapper.updateLastLoginTime(user.getId(), LocalDateTime.now());
        }

        StpUtil.login(user.getId());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("user", user);
        result.put("isNew", isNew);
        return result;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public AppUser getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }
}
