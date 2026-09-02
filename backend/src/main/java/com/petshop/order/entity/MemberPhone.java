package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MemberPhone extends BaseEntity {

    /** 归属门店（同一手机号在不同店可对应不同会员） */
    private Long shopId;
    private Long memberId;
    private String phone;
}
