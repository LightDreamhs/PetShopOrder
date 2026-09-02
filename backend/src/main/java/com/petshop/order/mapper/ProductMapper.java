package com.petshop.order.mapper;

import com.petshop.order.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    /** Admin 商品目录列表（平台级，SKU 聚合值按当前店统计） */
    List<Product> selectPageList(@Param("keyword") String keyword,
                                 @Param("type") String type,
                                 @Param("status") String status,
                                 @Param("shopId") Long shopId);

    /** C 端商品列表：商品在售 且 当前店存在在售 SKU */
    List<Product> selectAppPageList(@Param("type") String type,
                                    @Param("shopId") Long shopId);

    /** 详情（SKU 按店过滤）；onlyOnSale=true 时仅返回在售 SKU（C 端用） */
    Product selectById(@Param("id") Long id,
                       @Param("shopId") Long shopId,
                       @Param("onlyOnSale") boolean onlyOnSale);

    int insert(Product product);

    int updateById(Product product);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    int existsOrderItemByProductId(@Param("id") Long id);
}
