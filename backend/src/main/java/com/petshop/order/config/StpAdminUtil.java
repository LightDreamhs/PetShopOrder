package com.petshop.order.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 管理端独立账号体系（loginType=admin）。
 *
 * <p>与 C 端默认体系（StpUtil，loginType=login）完全隔离：token 服务端按
 * loginType 分键存储，客户端 cookie 名为 satoken-admin（匿名子类重写
 * splicingKeyTokenName，官方多账号方案），其余配置继承全局 sa-token.*。
 *
 * <p>背景：历史上两套账号共用同一 loginType，app_user.id 与 admin_user.id
 * 撞号时 C 端顾客的登录态会被管理端解析成对应管理员（身份混淆/提权）。
 * 管理端代码必须使用本类而非 StpUtil。
 */
public final class StpAdminUtil {

    public static final StpLogic stpLogic = new StpLogic("admin") {
        @Override
        public String splicingKeyTokenName() {
            // 独立 cookie 名，避免与 C 端 satoken 冲突/覆盖，双端可同时登录
            return super.splicingKeyTokenName() + "-admin";
        }
    };

    private StpAdminUtil() {
    }

    public static void login(Object id, String device) {
        stpLogic.login(id, device);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    public static SaSession getSession() {
        return stpLogic.getSession();
    }

    public static void checkRole(String role) {
        stpLogic.checkRole(role);
    }

    public static boolean hasRole(String role) {
        return stpLogic.hasRole(role);
    }
}
