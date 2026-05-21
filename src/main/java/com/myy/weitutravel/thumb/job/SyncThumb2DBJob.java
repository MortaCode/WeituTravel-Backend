package com.myy.weitutravel.thumb.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.common.utils.RedisUtil;
import com.myy.weitutravel.thumb.entity.Blog;
import com.myy.weitutravel.thumb.entity.Thumb;
import com.myy.weitutravel.thumb.service.BlogService;
import com.myy.weitutravel.thumb.service.ThumbService;
import com.myy.weitutravel.thumb.vo.ThumbEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时将 Redis 中的临时点赞数据同步到数据库
 *
 */
@Slf4j
@Component
public class SyncThumb2DBJob {

    @Resource
    private ThumbService thumbService;

    @Resource
    private BlogService blogService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        log.info("SyncThumb2DBJob 初始化完成，定时任务已注册");
    }

    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("开始执行");
        DateTime nowDate = DateUtil.date();
        // 如果秒数为0~9 则回到上一分钟的50秒
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            // 回到上一分钟
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "yyyy-MM-dd HH:mm:") + second;
        syncThumb2DBByDate(date);
        log.info("临时数据同步完成");
    }

    public void syncThumb2DBByDate(String date) {
        // 获取到临时点赞和取消点赞数据
        String tempThumbKey = RedisUtil.tempThumbKey(date);
        Map<Object, Object> allTempThumbMap = redisTemplate.opsForHash().entries(tempThumbKey);
        log.info("allTempThumbMap={}", allTempThumbMap);
        boolean thumbMapEmpty = CollUtil.isEmpty(allTempThumbMap);

        // 同步 点赞 到数据库
        Map<String, Long> blogThumbCountMap = new HashMap<>();
        if (thumbMapEmpty) {
            return;
        }
        ArrayList<Thumb> thumbList = new ArrayList<>();
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        boolean needRemove = false;
        for (Object userIdBlogIdObj : allTempThumbMap.keySet()) {
            String userIdBlogId = (String) userIdBlogIdObj;
            String[] userIdAndBlogId = userIdBlogId.split(StrPool.COLON);
            String userId = userIdAndBlogId[0];
            String blogId = userIdAndBlogId[1];
            // -1 取消点赞，1 点赞
            Long thumbType = Long.valueOf(allTempThumbMap.get(userIdBlogId).toString());
            if (thumbType == ThumbEnum.INCR.getValue()) {
                Thumb thumb = new Thumb();
                thumb.setId(IdUtil.objectId());
                thumb.setUserid(userId);
                thumb.setBlogid(blogId);
                thumbList.add(thumb);
            } else if (thumbType == ThumbEnum.DECR.getValue()) {
                // 拼接查询条件，批量删除
                needRemove = true;
                wrapper.or().eq(Thumb::getUserid, userId).eq(Thumb::getBlogid, blogId);
            } else {
                if (thumbType != ThumbEnum.NON.getValue()){
                    log.warn("数据异常：{}", userId + "," + blogId + "," + thumbType);
                }
                continue;
            }
            // 计算点赞增量
            blogThumbCountMap.put(blogId, blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType);
        }
        // 批量插入
        thumbService.saveBatch(thumbList);
        // 批量删除
        if (needRemove) {
            thumbService.remove(wrapper);
        }
        // 批量更新博客点赞量
        if (!blogThumbCountMap.isEmpty()) {
            batchUpdateThumbCount(blogThumbCountMap);
        }
        //删除
        redisTemplate.delete(tempThumbKey);
    }

    /**
     * 批量更新博客点赞数
     * @param thumbCountMap
     */
    void batchUpdateThumbCount(Map<String, Long> thumbCountMap) {
        if (thumbCountMap.isEmpty()) return;

        List<Blog> blogList = thumbCountMap.entrySet().stream()
                .map(entry -> {
                    Blog blog = new Blog();
                    blog.setId(entry.getKey());
                    blog.setThumbcount(entry.getValue());
                    return blog;
                })
                .collect(Collectors.toList());
        blogService.updateBatchById(blogList, 1000);
    }
}

