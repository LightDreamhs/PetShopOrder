package com.petshop.order.config;

import cn.hutool.crypto.digest.BCrypt;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.entity.Category;
import com.petshop.order.mapper.AdminUserMapper;
import com.petshop.order.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public void run(String... args) {
        initBossAccount();
        initCategories();
        log.info("数据初始化完成");
    }

    private void initBossAccount() {
        AdminUser existing = adminUserMapper.selectByUsername("admin");
        if (existing != null) {
            return;
        }
        AdminUser boss = new AdminUser();
        boss.setUsername("admin");
        boss.setPasswordHash(BCrypt.hashpw("admin123"));
        boss.setRealName("管理员");
        boss.setRole("BOSS");
        boss.setStatus(1);
        adminUserMapper.insert(boss);
        log.info("已创建默认 BOSS 账号: admin / admin123");
    }

    private void initCategories() {
        if (!categoryMapper.selectList(null).isEmpty()) {
            return;
        }
        Category goods = new Category();
        goods.setName("商品");
        goods.setType("GOODS");
        goods.setSort(1);
        categoryMapper.insert(goods);

        Category service = new Category();
        service.setName("服务");
        service.setType("SERVICE");
        service.setSort(2);
        categoryMapper.insert(service);
        log.info("已创建默认分类: 商品、服务");
    }
}
