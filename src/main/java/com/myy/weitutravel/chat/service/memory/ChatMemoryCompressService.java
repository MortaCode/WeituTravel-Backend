package com.myy.weitutravel.chat.service.memory;

import lombok.AllArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 聊天记忆压缩
 */
@Service
@AllArgsConstructor
public class ChatMemoryCompressService {

    private final ChatMemoryService chatMemoryService;
    private final CompressionService compressionService;

    private static final boolean ENABLE_COMPRESSION = true;  // 是否启用压缩

    /**
     * 获取会话记忆（带压缩）
     */
    public List<Message> getMemoryWithCompress(String sessionId) {
        // 原有获取逻辑
        List<Message> messages = chatMemoryService.getMemory(sessionId);

        // 启用压缩且消息数超过阈值
        if (ENABLE_COMPRESSION) {
            messages = compressionService.compressIfNeeded(sessionId, messages);
            // 更新缓存为压缩后的版本
            chatMemoryService.updateLocalCache(sessionId, messages);
            chatMemoryService.updateRedisCache(sessionId, messages);
        }

        return messages;
    }

    /**
     * 获取指定窗口的记忆（滑动窗口）
     */
    public List<Message> getMemoryWithWindow(String sessionId, int windowSize) {
        List<Message> allMessages = chatMemoryService.getMemory(sessionId);

        if (allMessages.size() <= windowSize) {
            return allMessages;
        }

        // 滑动窗口：保留最近的消息
        return allMessages.subList(allMessages.size() - windowSize, allMessages.size());
    }


    /**
     * 获取重要记忆（基于消息重要性评分）
     */
    public List<Message> getImportantMemories(String sessionId, int maxCount) {
        List<Message> allMessages = chatMemoryService.getMemory(sessionId);

        // 简单的重要消息识别逻辑
        // 可以扩展为：基于关键词、长度、是否包含问题等
        return allMessages.stream()
                .filter(msg -> isImportantMessage(msg))
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    /**
     * 判断是否重要消息
     */
    private boolean isImportantMessage(Message msg) {
        String text = msg.getText();
        // 包含关键信息
        if (text.contains("重要") || text.contains("关键") || text.contains("记住")) {
            return true;
        }
        // 长度较长，包含详细信息
        if (text.length() > 100) {
            return true;
        }
        // 包含问题
        if (text.contains("？") || text.contains("?")) {
            return true;
        }
        return false;
    }


//    /**
//     * 异步保存时进行压缩存储
//     */
//    private void persistToDatabase(String sessionId, List<Message> messages) {
//        // ... 原有保存逻辑 ...
//
//        // 如果需要，保存压缩版本到快照表
//        if (ENABLE_COMPRESSION && messages.size() > COMPRESSION_THRESHOLD) {
//            saveCompressedSnapshot(sessionId, messages);
//        }
//    }
//
//    /**
//     * 保存压缩快照
//     */
//    private void saveCompressedSnapshot(String sessionId, List<Message> messages) {
//        try {
//            MemoryCompressionService.CompressedMemory compressed =
//                    compressionService.getCompressedMemory(sessionId);
//
//            if (compressed != null && compressed.getSummary() != null) {
//                ChatSnapshot snapshot = new ChatSnapshot();
//                snapshot.setId(IdUtil.objectId());
//                snapshot.setSessionId(sessionId);
//                snapshot.setSummary(compressed.getSummary());
//                snapshot.setMessageCount(messages.size());
//                snapshot.setCompressCount(compressed.getCompressionCount());
//                snapshot.setCreateTime(LocalDateTime.now());
//
//                // 使用已有的 snapshotService 保存
//                // snapshotService.save(snapshot);
//            }
//        } catch (Exception e) {
//            log.error("保存压缩快照失败", e);
//        }
//    }

}
