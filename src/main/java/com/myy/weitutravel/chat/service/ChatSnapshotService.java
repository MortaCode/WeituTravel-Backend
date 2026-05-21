package com.myy.weitutravel.chat.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myy.weitutravel.chat.entity.ChatSnapshot;
import com.myy.weitutravel.chat.mapper.ChatSnapshotMapper;
import org.springframework.stereotype.Service;

/**
* @author ADMIN
* @description 针对表【snapshot(会话记忆快照表)】的数据库操作Service
* @createDate 2026-04-02 02:44:27
*/
@Service
public class ChatSnapshotService extends ServiceImpl<ChatSnapshotMapper, ChatSnapshot> {

}
