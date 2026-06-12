package com.myy.weitutravel.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myy.weitutravel.product.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE t_product SET stock = stock - #{count}, sales = sales + #{count} WHERE id = #{id} AND stock >= #{count}")
    int deductStock(@Param("id") String id, @Param("count") int count);
}
