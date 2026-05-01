package com.petshop.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.AppUser;
import com.petshop.order.mapper.AppUserMapper;
import com.petshop.order.service.AppAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private final AppUserMapper appUserMapper;

    // 开发期：内存存储验证码
    private final Map<String, String> codeStore = new ConcurrentHashMap<>();

    @Override
    public void sendSmsCode(String phone) {
        // 开发期固定验证码 1234
        String code = "1234";
        codeStore.put(phone, code);
        log.info("[SMS] phone={}, code={}", phone, code);
    }

    @Override
    public AppUser login(String phone, String code) {
        String stored = codeStore.get(phone);
        if (stored == null || !stored.equals(code)) {
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
        codeStore.remove(phone);
        return user;
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
