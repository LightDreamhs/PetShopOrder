package com.petshop.order.mapper;

import com.petshop.order.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> selectPageList(@Param("keyword") String keyword,
                                 @Param("type") String type,
                                 @Param("status") String status);

    Product selectById(@Param("id") Long id);

    int insert(Product product);

    int updateById(Product product);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    int existsOrderItemByProductId(@Param("id") Long id);
}
