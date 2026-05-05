package com.petshop.order.config;

import cn.dev33.satoken.stp.StpInterface;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.mapper.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        AdminUser user = adminUserMapper.selectById(Long.parseLong(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        return List.of(user.getRole());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
