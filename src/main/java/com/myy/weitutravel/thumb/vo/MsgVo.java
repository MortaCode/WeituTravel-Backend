package com.myy.weitutravel.thumb.vo;

import lombok.Data;

@Data
public class MsgVo {

    /**
     * 提醒消息
     */
    private String msg;

    /**
     * 执行记录
     */
    private String id;


    public static MsgVo of(String msg, String id){
        MsgVo msgVo = new MsgVo();
        msgVo.setMsg(msg);
        msgVo.setId(id);
        return msgVo;
    }
}
