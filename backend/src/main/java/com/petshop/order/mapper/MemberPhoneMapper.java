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

    int countByPhone(@Param("phone") String phone, @Param("excludeMemberId") Long excludeMemberId);

    Long selectMemberIdByPhone(@Param("phone") String phone);
}
