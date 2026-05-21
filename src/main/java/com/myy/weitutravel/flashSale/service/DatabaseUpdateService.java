package com.myy.weitutravel.flashSale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.flashSale.entity.Stock;
import com.myy.weitutravel.flashSale.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第三层防线：MySQL乐观锁兜底
 * 确保绝对不超卖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseUpdateService {

    private final StockMapper stockMapper;

    /**
     * 使用乐观锁扣减库存（终极防线）
     * @return true-扣减成功，false-库存不足或并发冲突
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockWithOptimisticLock(String stockId) {
        // 使用 stock > 0 条件 + 数据库行锁保证不超卖
        int affectedRows = stockMapper.deductStockOptimistic(stockId);

        if (affectedRows > 0) {
            log.info("MySQL库存扣减成功: stockId={}", stockId);
            return true;
        }

        log.warn("MySQL库存扣减失败，库存不足或并发: stockId={}", stockId);
        return false;
    }

    /**
     * 初始化数据库库存
     */
    public void initDatabaseStock(String stockId, int stock) {
        Stock stockEntity = new Stock();
        stockEntity.setStockId(stockId);
        stockEntity.setStock(stock);
        stockEntity.setVersion(0);

        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getStockId, stockId);

        if (stockMapper.selectCount(wrapper) == 0) {
            stockMapper.insert(stockEntity);
            log.info("数据库库存初始化完成: stockId={}, stock={}", stockId, stock);
        }
    }
}
