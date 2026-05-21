package com.myy.weitutravel.common.utils;

import com.myy.weitutravel.common.constants.Constants;

public class RedisUtil {

    /**
     * 点赞key
     * @param userid
     * @return
     */
    public static String thumbKey(String userid){
        return Constants.THUMB_KEY_PREFIX + userid;
    }

    /**
     * 临时点赞key
     * @param
     * @return
     */
    public static String tempThumbKey(String time){
        return Constants.TEMP_THUMB_KEY_PREFIX.formatted(time);
    }
}
