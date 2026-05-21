package com.myy.weitutravel.thumb.controller;

import cn.hutool.core.collection.CollUtil;
import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.thumb.service.BlogService;
import com.myy.weitutravel.thumb.service.ThumbUPService;
import com.myy.weitutravel.thumb.vo.BlogVo;
import com.myy.weitutravel.thumb.vo.MsgVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping("blog")
public class BlogController {

    private final BlogService blogService;
    private final ThumbUPService thumbUPService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取博客
     * @param request
     * @param blogId
     * @return
     */
    @GetMapping("searchById")
    public Result<BlogVo> searchById(HttpServletRequest request, String blogId){
        return Result.success(blogService.searchById(request, blogId));
    }

    /**
     * 获取博客列表
     * @param request
     * @param blogIds 逗号拼接
     * @return
     */
    @GetMapping("searchByIds")
    public Result<List<BlogVo>> searchByIds(HttpServletRequest request, String blogIds){
        return Result.success(blogService.searchByIds(request, blogIds));
    }

    /**
     * 点赞和取消点赞
     */
    @GetMapping("thumb")
    public Result<MsgVo> thumb(HttpServletRequest request, String blogId){
        return Result.success(thumbUPService.thumb(request, blogId));
    }

    /**
     * 清空点赞相关数据
     */
    @GetMapping("/clearThumbData")
    @Operation(summary = "清空点赞相关数据", description = "清空所有点赞相关的缓存")
    public Result<Integer> clearThumbData() {
        try {
            // 清空临时点赞数据
            Set<String> tempThumbKeys = redisTemplate.keys("temp:thumb:*");
            // 清空用户点赞记录
            Set<String> userThumbKeys = redisTemplate.keys("thumb:*");

            int totalCount = 0;
            if (CollUtil.isNotEmpty(tempThumbKeys)) {
                Long count = redisTemplate.delete(tempThumbKeys);
                totalCount += count;
                log.info("清空临时点赞数据: {}", count);
            }
            if (CollUtil.isNotEmpty(userThumbKeys)) {
                Long count = redisTemplate.delete(userThumbKeys);
                totalCount += count;
                log.info("清空用户点赞记录: {}", count);
            }

            return Result.success("清空成功");
        } catch (Exception e) {
            log.error("清空点赞数据失败", e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }

}
