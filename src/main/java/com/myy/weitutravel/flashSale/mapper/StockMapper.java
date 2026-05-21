package com.myy.weitutravel.flashSale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myy.weitutravel.flashSale.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Update("UPDATE t_stock SET stock = stock - 1, update_time = NOW() " +
            "WHERE stock_id = #{stockId} AND stock > 0")
    int deductStockOptimistic(String stockId);
}
