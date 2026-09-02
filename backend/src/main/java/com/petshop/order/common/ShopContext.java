package com.petshop.order.common;

/**
 * 当前请求的门店上下文（ThreadLocal）。
 * 由 ShopResolveInterceptor 在请求进入时写入、结束时清理；
 * 业务层通过 get()/require() 读取当前店铺。
 *
 * 注意：@Async 线程（如订单通知）不继承本上下文，应改用业务对象自身携带的
 * shopId（如 Orders.getShopId()），不要在异步线程读取本类。
 */
public final class ShopContext {

    private static final ThreadLocal<Long> CURRENT_SHOP = new ThreadLocal<>();

    private ShopContext() {
    }

    public static void set(Long shopId) {
        CURRENT_SHOP.set(shopId);
    }

    /** 当前店铺 id，可能为 null（库中尚无任何门店时） */
    public static Long get() {
        return CURRENT_SHOP.get();
    }

    /** 当前店铺 id，业务写路径必须存在 */
    public static Long require() {
        Long shopId = CURRENT_SHOP.get();
        if (shopId == null) {
            throw new BusinessException("当前请求未解析到门店，请检查门店配置");
        }
        return shopId;
    }

    public static void clear() {
        CURRENT_SHOP.remove();
    }
}
