package com.petshop.order.entity;

import com.petshop.order.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Member extends BaseEntity {

    /** 归属门店（会员体系店铺级） */
    private Long shopId;
    private String name;
    private Long levelId;
    private String remark;
    private List<String> phones;
    private String levelName;
}
