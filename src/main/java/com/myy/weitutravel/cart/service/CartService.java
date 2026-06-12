package com.myy.weitutravel.cart.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.cart.entity.CartItem;
import com.myy.weitutravel.cart.mapper.CartItemMapper;
import com.myy.weitutravel.common.exception.BizException;
import com.myy.weitutravel.product.entity.Product;
import com.myy.weitutravel.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public List<CartItem> list(String userId) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getCreateTime));

        for (CartItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p != null) {
                item.setProductName(p.getName());
                item.setProductImage(p.getImage());
                item.setProductPrice(p.getPrice());
            }
        }
        return items;
    }

    @Transactional
    public void add(String userId, String productId, int quantity) {
        Product p = productMapper.selectById(productId);
        if (p == null || p.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }
        if (quantity > p.getStock()) {
            throw new BizException("库存不足，当前库存: " + p.getStock());
        }

        CartItem exist = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getProductId, productId));

        if (exist != null) {
            int newQty = exist.getQuantity() + quantity;
            if (newQty > p.getStock()) {
                throw new BizException("库存不足，当前库存: " + p.getStock());
            }
            exist.setQuantity(newQty);
            cartItemMapper.updateById(exist);
        } else {
            CartItem item = new CartItem();
            item.setId(IdUtil.fastSimpleUUID());
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setSelected(1);
            cartItemMapper.insert(item);
        }
    }

    @Transactional
    public void updateQuantity(String userId, String cartItemId, int quantity) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        if (quantity <= 0) {
            cartItemMapper.deleteById(cartItemId);
            return;
        }
        Product p = productMapper.selectById(item.getProductId());
        if (quantity > p.getStock()) {
            throw new BizException("库存不足");
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Transactional
    public void toggleSelect(String userId, String cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        item.setSelected(item.getSelected() == 1 ? 0 : 1);
        cartItemMapper.updateById(item);
    }

    @Transactional
    public void remove(String userId, String cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        cartItemMapper.deleteById(cartItemId);
    }

    public List<CartItem> getSelectedItems(String userId) {
        return cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getSelected, 1));
    }
}
