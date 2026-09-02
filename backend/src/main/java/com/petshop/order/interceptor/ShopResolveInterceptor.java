package com.petshop.order.interceptor;

import com.petshop.order.config.StpAdminUtil;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.entity.Shop;
import com.petshop.order.mapper.AdminUserMapper;
import com.petshop.order.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 门店上下文解析（注册于 /api/app/** 与 /api/admin/**，在 SaInterceptor 之后执行）。
 *
 * - C 端 /api/app/**：请求头 X-Shop-Code 指定门店编码，缺失/无效时回退默认店。
 *   当前店是公开信息，未登录用户也可多店浏览；写接口（下单/预约）另校验门店营业状态。
 * - 管理端 /api/admin/**：登录员工的 shopId（登录时写入 Sa-Token session）；
 *   BOSS（shop_id 为 NULL 的总部账号）可通过请求头 X-Shop-Id 切换门店。
 *
 * 请求结束后清理 ThreadLocal，防止线程池复用串店。
 */
@Component
@RequiredArgsConstructor
public class ShopResolveInterceptor implements HandlerInterceptor {

    public static final String SHOP_CODE_HEADER = "X-Shop-Code";
    public static final String SHOP_ID_HEADER = "X-Shop-Id";

    private final ShopService shopService;
    private final AdminUserMapper adminUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        Long shopId = uri.startsWith("/api/admin/")
                ? resolveAdminShopId(request)
                : resolveAppShopId(request);
        ShopContext.set(shopId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ShopContext.clear();
    }

    private Long resolveAppShopId(HttpServletRequest request) {
        Shop shop = shopService.resolveByCode(request.getHeader(SHOP_CODE_HEADER));
        if (shop == null) {
            shop = shopService.getDefaultShop();
        }
        return shop != null ? shop.getId() : null;
    }

    private Long resolveAdminShopId(HttpServletRequest request) {
        Long employeeShopId = null;
        try {
            if (StpAdminUtil.isLogin()) {
                Object cached = StpAdminUtil.getSession().get("shopId");
                if (cached instanceof Number number) {
                    // 0=总部（BOSS）
                    employeeShopId = number.longValue() == 0L ? null : number.longValue();
                } else {
                    // 老会话没有 shopId 属性：查库补写
                    AdminUser user = adminUserMapper.selectById(StpAdminUtil.getLoginIdAsLong());
                    if (user != null) {
                        employeeShopId = user.getShopId();
                        StpAdminUtil.getSession().set("shopId", employeeShopId != null ? employeeShopId : 0L);
                    }
                }
            }
        } catch (Exception ignored) {
            // 未登录（如 admin 登录接口本身）走默认店
        }

        if (employeeShopId != null) {
            return employeeShopId;
        }
        // 总部（BOSS）或未登录：允许 X-Shop-Id 切换，否则默认店
        Long headerShopId = parseLong(request.getHeader(SHOP_ID_HEADER));
        if (headerShopId != null) {
            Shop shop = shopService.getById(headerShopId);
            if (shop != null) {
                return shop.getId();
            }
        }
        Shop defaultShop = shopService.getDefaultShop();
        return defaultShop != null ? defaultShop.getId() : null;
    }

    private Long parseLong(String val) {
        if (val == null || val.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
