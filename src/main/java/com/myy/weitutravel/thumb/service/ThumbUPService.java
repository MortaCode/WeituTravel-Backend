package com.myy.weitutravel.thumb.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.common.utils.RedisUtil;
import com.myy.weitutravel.thumb.entity.Thumb;
import com.myy.weitutravel.thumb.mapper.ThumbMapper;
import com.myy.weitutravel.thumb.vo.LuaStateEnum;
import com.myy.weitutravel.thumb.vo.MsgVo;
import com.myy.weitutravel.user.entity.User;
import com.myy.weitutravel.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;

/**
 * 事先缓存。事后定时入库
 */
@Slf4j
@Service
@AllArgsConstructor
public class ThumbUPService  extends ServiceImpl<ThumbMapper, Thumb> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final DefaultRedisScript<Long> thumbScript;
    private final DefaultRedisScript<Long> unthumbScript;

    /**
     * 点赞和取消点赞
     */
    @Transactional
    public MsgVo thumb(HttpServletRequest request, String blogId) {
        if (request == null || !StringUtils.hasText(blogId)){
            throw new RuntimeException("参数有误");
        }
        User user = userService.getLoginUser(request);
        Assert.notNull(user, "请登录!");

        String thumbKey = RedisUtil.thumbKey(user.getId());
        String tempThumbKey = RedisUtil.tempThumbKey(timeslice());

        synchronized (user.getId().toString().intern()){

            boolean hasThumb = hasThumb(user.getId(), blogId);

            if (!hasThumb){
                long result = redisTemplate.execute(
                        thumbScript,
                        Arrays.asList(tempThumbKey, thumbKey),
                        user.getId(),
                        blogId
                );
                return MsgVo.of(result == LuaStateEnum.SUCCESS.getValue() ? "点赞成功" : "点赞失败", "");
            } else {
                long result = redisTemplate.execute(
                    unthumbScript,
                    Arrays.asList(tempThumbKey, thumbKey),
                    user.getId(),
                    blogId
                );
                return MsgVo.of(result == LuaStateEnum.SUCCESS.getValue() ? "取消点赞成功" : "取消点赞失败", "");
            }
        }
    }

    private String timeslice(){
        Date nowDate = DateUtil.date();
        return DateUtil.format(nowDate, "yyyy-MM-dd HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    /**
     * 是否已点赞
     * @param userId
     * @param blogId
     * @return
     */
    private boolean hasThumb(String userId, String blogId) {
        Boolean hasThumb = redisTemplate.opsForHash()
                .hasKey(Constants.THUMB_KEY_PREFIX + userId, blogId);
        if (!hasThumb){
            hasThumb = this.baseMapper.exists(new LambdaQueryWrapper<>(Thumb.class)
                    .eq(Thumb::getUserid, userId)
                    .eq(Thumb::getBlogid, blogId));
        }
        return hasThumb;
    }
}
