package com.petshop.order.mapper;

import com.petshop.order.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper {

    List<Member> selectPageList(@Param("keyword") String keyword, @Param("levelId") Long levelId);

    Member selectById(@Param("id") Long id);

    int insert(Member member);

    int updateById(Member member);

    int deleteById(@Param("id") Long id);
}
