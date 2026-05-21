package com.myy.weitutravel.thumb.vo;

import lombok.Getter;

@Getter
public enum ThumbEnum {

    INCR(1),
    DECR(-1),
    NON(-1);

    private long value;

    ThumbEnum(int value){
        this.value = value;
    }
}
