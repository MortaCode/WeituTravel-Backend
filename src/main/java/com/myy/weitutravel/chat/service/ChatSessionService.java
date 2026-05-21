package com.myy.weitutravel.chat.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myy.weitutravel.chat.entity.ChatSession;
import com.myy.weitutravel.chat.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

/**
* @author ADMIN
* @description 针对表【session(对话会话表)】的数据库操作Service
* @createDate 2026-04-02 02:43:53
*/
@Service
public class ChatSessionService extends ServiceImpl<ChatSessionMapper, ChatSession> {

}
