package com.myy.weitutravel.thumb.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.thumb.entity.Blog;
import com.myy.weitutravel.thumb.entity.Thumb;
import com.myy.weitutravel.thumb.mapper.BlogMapper;
import com.myy.weitutravel.thumb.mapper.ThumbMapper;
import com.myy.weitutravel.thumb.vo.BlogVo;
import com.myy.weitutravel.user.entity.User;
import com.myy.weitutravel.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author ADMIN
* @description 针对表【blog】的数据库操作Service
* @createDate 2026-04-12 15:45:17
*/
@Service
@AllArgsConstructor
public class BlogService extends ServiceImpl<BlogMapper, Blog> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final ThumbMapper thumbMapper;

    /**
     * 获取博客列表
     * @param request
     * @param blogIds 逗号拼接
     * @return
     */
    public List<BlogVo> searchByIds(HttpServletRequest request, String blogIds) {

        Assert.hasLength(blogIds, "参数为空");
        List<String> ids = Arrays.asList(blogIds.split(","));

        List<Blog> blogs = lambdaQuery().in(Blog::getId, ids).list();
        if (CollectionUtils.isEmpty(blogs)) {
            return Collections.emptyList();
        }
        List<BlogVo> blogList = BeanUtil.copyToList(blogs, BlogVo.class);

        User user = userService.getLoginUser(request);
        if (user == null) {
            return blogList;
        }

        Set<String> thumbSet = thumbSet(user.getId(), ids);

        blogList.forEach(blog -> blog.setHasThumb(thumbSet.contains(blog.getId())));
        return blogList;
    }

    /**
     * 点赞数据
     */
    private Set<String> thumbSet(String userId, List<String> blogIds) {
        if (CollectionUtils.isEmpty(blogIds)) {
            return Collections.emptySet();
        }

        String cacheKey = Constants.THUMB_KEY_PREFIX + userId;
        List<Object> cachedResults = redisTemplate.opsForHash().multiGet(cacheKey,
                blogIds.stream().map(Object.class::cast).collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(cachedResults)) {
            return thumbMapper.selectList(new LambdaQueryWrapper<>(Thumb.class)
                    .in(Thumb::getBlogid, blogIds)
                    .eq(Thumb::getUserid, userId)
            ).stream().map(Thumb::getBlogid).collect(Collectors.toSet());
        }

        return cachedResults.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    /**
     * 获取博客
     * @param request
     * @param blogId
     * @return
     */
    public BlogVo searchById(HttpServletRequest request, String blogId) {
        Assert.hasLength(blogId, "参数为空");
        Blog blog = this.getById(blogId);
        User user = userService.getLoginUser(request);
        BlogVo blogVo = BeanUtil.toBean(blog, BlogVo.class);
        if (user == null || blogVo == null){
            return blogVo;
        }
        blogVo.setHasThumb(hasThumb(user.getId(), blogVo.getId()));
        return blogVo;
    }


    private boolean hasThumb(String userId, String blogId) {
        Boolean hasThumb = redisTemplate.opsForHash()
                .hasKey(Constants.THUMB_KEY_PREFIX + userId, blogId);
        if (!hasThumb){
            hasThumb = thumbMapper.exists(new LambdaQueryWrapper<>(Thumb.class)
                            .eq(Thumb::getUserid, userId)
                            .eq(Thumb::getBlogid, blogId));

        }
        return hasThumb;
    }


}
