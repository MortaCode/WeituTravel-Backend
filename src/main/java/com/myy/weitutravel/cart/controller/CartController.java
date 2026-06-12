package com.myy.weitutravel.cart.controller;

import com.myy.weitutravel.cart.entity.CartItem;
import com.myy.weitutravel.cart.service.CartService;
import com.myy.weitutravel.cart.vo.CartAddVo;
import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.login.entity.User;
import com.myy.weitutravel.login.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping("list")
    public Result<List<CartItem>> list(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return Result.success(cartService.list(user.getId()));
    }

    @PostMapping("add")
    public Result<String> add(@Validated @RequestBody CartAddVo vo, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        cartService.add(user.getId(), vo.getProductId(), vo.getQuantity());
        return Result.success("加入成功");
    }

    @PutMapping("item/{id}/qty/{quantity}")
    public Result<String> updateQty(@PathVariable String id, @PathVariable int quantity,
                                     HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        cartService.updateQuantity(user.getId(), id, quantity);
        return Result.success("更新成功");
    }

    @PutMapping("item/{id}/toggle")
    public Result<String> toggleSelect(@PathVariable String id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        cartService.toggleSelect(user.getId(), id);
        return Result.success("操作成功");
    }

    @DeleteMapping("item/{id}")
    public Result<String> remove(@PathVariable String id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        cartService.remove(user.getId(), id);
        return Result.success("删除成功");
    }
}
