package com.myy.weitutravel.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.product.entity.Product;
import com.myy.weitutravel.product.service.ProductService;
import com.myy.weitutravel.product.vo.ProductSaveVo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("product")
public class ProductController {

    private final ProductService productService;

    @GetMapping("list")
    public Result<IPage<Product>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(productService.page(page, size, category, keyword));
    }

    @GetMapping("detail/{id}")
    public Result<Product> detail(@PathVariable String id) {
        return Result.success(productService.detail(id));
    }

    @PostMapping("create")
    public Result<Product> create(@Validated @RequestBody ProductSaveVo vo) {
        return Result.success(productService.create(vo));
    }

    @PutMapping("update/{id}")
    public Result<Product> update(@PathVariable String id, @Validated @RequestBody ProductSaveVo vo) {
        return Result.success(productService.update(id, vo));
    }

    @PutMapping("off/{id}")
    public Result<String> offShelf(@PathVariable String id) {
        productService.offShelf(id);
        return Result.success("下架成功");
    }
}
