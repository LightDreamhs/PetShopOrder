package com.petshop.order.mapper;

import com.petshop.order.entity.MemberPhone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberPhoneMapper {

    List<MemberPhone> selectByMemberId(@Param("memberId") Long memberId);

    int insert(MemberPhone memberPhone);

    int deleteByMemberId(@Param("memberId") Long memberId);

    int countByPhone(@Param("phone") String phone,
                     @Param("excludeMemberId") Long excludeMemberId,
                     @Param("shopId") Long shopId);

    /** 按店命中会员（会员体系店铺级） */
    Long selectMemberIdByShopAndPhone(@Param("shopId") Long shopId, @Param("phone") String phone);

    List<String> selectPhonesByMemberId(@Param("memberId") Long memberId);
}
