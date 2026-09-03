package com.petshop.order.service;

import com.petshop.order.entity.Shop;

import java.util.List;

public interface ShopService {

    Shop getById(Long id);

    /** 按编码找店（编码不区分大小写），找不到返回 null */
    Shop resolveByCode(String code);

    /** 默认店：sort 最小、id 最小的门店，可能为 null（库中无门店） */
    Shop getDefaultShop();

    List<Shop> listAll();

    /** 当前上下文门店，不存在抛业务异常 */
    Shop requireCurrentShop();

    /** 校验门店存在且营业中（下单/预约等写路径用） */
    Shop requireOpenShop(Long shopId);

    /** 新建门店（BOSS）：编码唯一校验 + 默认店铺配置落库 */
    Shop create(Shop shop);

    /** 编辑门店（BOSS）：坐标同步到店铺配置 */
    Shop update(Long id, Shop shop);

    /** 营业/歇业切换（BOSS） */
    void updateStatus(Long id, String status);
}
