package com.myy.weitutravel.product.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myy.weitutravel.common.exception.BizException;
import com.myy.weitutravel.product.entity.Product;
import com.myy.weitutravel.product.mapper.ProductMapper;
import com.myy.weitutravel.product.vo.ProductSaveVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public IPage<Product> page(int pageNum, int pageSize, String category, String keyword) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, 1);
        if (category != null && !category.isBlank()) {
            qw.eq(Product::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getName, keyword);
        }
        qw.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public Product detail(String id) {
        Product p = productMapper.selectById(id);
        if (p == null || p.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }
        return p;
    }

    @Transactional
    public Product create(ProductSaveVo vo) {
        Product p = new Product();
        p.setId(IdUtil.fastSimpleUUID());
        p.setName(vo.getName());
        p.setDescription(vo.getDescription());
        p.setPrice(vo.getPrice());
        p.setStock(vo.getStock());
        p.setImage(vo.getImage());
        p.setImages(vo.getImages());
        p.setCategory(vo.getCategory());
        p.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
        p.setSales(0);
        productMapper.insert(p);
        log.info("商品上架: id={}, name={}", p.getId(), p.getName());
        return p;
    }

    @Transactional
    public Product update(String id, ProductSaveVo vo) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException("商品不存在");
        }
        p.setName(vo.getName());
        p.setDescription(vo.getDescription());
        p.setPrice(vo.getPrice());
        p.setStock(vo.getStock());
        p.setImage(vo.getImage());
        p.setImages(vo.getImages());
        p.setCategory(vo.getCategory());
        if (vo.getStatus() != null) {
            p.setStatus(vo.getStatus());
        }
        productMapper.updateById(p);
        return p;
    }

    @Transactional
    public void offShelf(String id) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException("商品不存在");
        }
        p.setStatus(0);
        productMapper.updateById(p);
    }

    @Transactional
    public int deductStock(String productId, int count) {
        int rows = productMapper.deductStock(productId, count);
        if (rows == 0) {
            throw new BizException("库存不足");
        }
        return rows;
    }
}
