package com.myy.weitutravel.chat.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myy.weitutravel.chat.entity.ChatMessage;
import com.myy.weitutravel.chat.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author ADMIN
* @description 针对表【message(对话消息表)】的数据库操作Service
* @createDate 2026-04-02 02:44:19
*/
@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

}
