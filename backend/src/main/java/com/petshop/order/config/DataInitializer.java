package com.petshop.order.config;

import cn.hutool.crypto.digest.BCrypt;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;

    @Value("${app.init.admin-username:admin}")
    private String adminUsername;

    @Value("${app.init.admin-password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 弱口令提醒：生产环境务必通过环境变量 APP_INIT_ADMIN_PASSWORD 设置强密码
        if (isWeakPassword(adminPassword)) {
            log.warn("⚠️ 检测到弱默认管理员口令，生产环境请务必通过环境变量 APP_INIT_ADMIN_PASSWORD 设置强密码");
        }
        initBossAccount();
        log.info("数据初始化完成");
    }

    private boolean isWeakPassword(String pwd) {
        return pwd == null || pwd.length() < 8 || "admin123".equals(pwd);
    }

    private void initBossAccount() {
        AdminUser existing = adminUserMapper.selectByUsername(adminUsername);
        if (existing != null) {
            return;
        }
        AdminUser boss = new AdminUser();
        boss.setUsername(adminUsername);
        boss.setPasswordHash(BCrypt.hashpw(adminPassword));
        boss.setRealName("管理员");
        boss.setRole("BOSS");
        boss.setStatus(1);
        adminUserMapper.insert(boss);
        log.info("已创建默认 BOSS 账号: {}", adminUsername);
    }
}
