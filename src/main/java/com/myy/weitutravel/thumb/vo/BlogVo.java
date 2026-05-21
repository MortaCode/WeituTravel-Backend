package com.myy.weitutravel.thumb.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class BlogVo {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     *
     */
    private String userid;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面
     */
    private String coverimg;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Long thumbcount;


    /**
     * 是否点赞
     */
    private boolean hasThumb;
}
