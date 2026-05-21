package com.myy.weitutravel.chat.service.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 压缩核心业务
 */
@Slf4j
@Service
@AllArgsConstructor
public class CompressionService {

    private final OpenAiChatModel chatModel;

    // 压缩配置
    private static final int MAX_CONTEXT_MESSAGES = 20;      // 最大保留消息数
    private static final int COMPRESSION_THRESHOLD = 30;     // 超过30条触发压缩
    private static final int KEEP_RECENT_COUNT = 10;         // 保留最近10条完整消息
    private static final int MAX_SUMMARY_LENGTH = 500;       // 摘要最大长度

    // 压缩后的记忆缓存
    private final Map<String, CompressedMemory> compressedMemoryCache = new ConcurrentHashMap<>();

    /**
     * 压缩会话记忆
     * @param sessionId 会话ID
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    public List<Message> compressIfNeeded(String sessionId, List<Message> messages) {
        if (messages.size() <= COMPRESSION_THRESHOLD) {
            return messages;
        }

        log.info("会话 {} 消息数达到 {}, 触发记忆压缩", sessionId, messages.size());

        // 获取或创建压缩记忆
        CompressedMemory compressed = compressedMemoryCache.computeIfAbsent(sessionId,
                k -> new CompressedMemory());

        // 提取新消息（上次压缩后的消息）
        List<Message> newMessages = messages.subList(compressed.getLastCompressedIndex(), messages.size());

        if (newMessages.size() >= COMPRESSION_THRESHOLD / 2) {
            // 生成摘要
            String newSummary = generateSummary(newMessages);

            // 合并摘要
            String mergedSummary = mergeSummaries(compressed.getSummary(), newSummary);
            compressed.setSummary(mergedSummary);
            compressed.setLastCompressedIndex(messages.size() - KEEP_RECENT_COUNT);
            compressed.setCompressTime(LocalDateTime.now());

            // 构建压缩后的消息列表：摘要 + 最近N条消息
            List<Message> compressedMessages = new ArrayList<>();

            // 添加摘要作为系统消息
            if (mergedSummary != null && !mergedSummary.isEmpty()) {
                compressedMessages.add(createSummaryMessage(mergedSummary));
            }

            // 添加最近的消息
            int startIndex = Math.max(0, messages.size() - KEEP_RECENT_COUNT);
            compressedMessages.addAll(messages.subList(startIndex, messages.size()));

            log.info("会话 {} 压缩完成: {} -> {} 条消息",
                    sessionId, messages.size(), compressedMessages.size());

            return compressedMessages;
        }

        return messages;
    }

    /**
     * 生成消息摘要
     */
    private String generateSummary(List<Message> messages) {
        try {
            String conversationText = messages.stream()
                    .map(msg -> {
                        String role = msg instanceof UserMessage ? "用户" : "助手";
                        return role + ": " + msg.getText();
                    })
                    .collect(Collectors.joining("\n"));

            String promptTemplate = """
                请总结以下对话的核心内容和关键信息，要求：
                1. 保留重要的决策、问题和答案
                2. 记录用户的关键需求和偏好
                3. 总结要简洁，不超过500字
                4. 使用第三人称叙述
                
                对话内容：
                {conversation}
                
                总结：
                """;

            PromptTemplate template = new PromptTemplate(promptTemplate);
            Prompt prompt = template.create(Map.of("conversation", conversationText));

            String summary = chatModel.call(prompt).getResult().getOutput().getText();

            // 限制摘要长度
            if (summary.length() > MAX_SUMMARY_LENGTH) {
                summary = summary.substring(0, MAX_SUMMARY_LENGTH) + "...";
            }

            return summary;

        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return "对话历史摘要生成失败，以下是最近的关键对话：" +
                    messages.stream().limit(5).map(Message::getText).collect(Collectors.joining(" | "));
        }
    }

    /**
     * 合并新旧摘要
     */
    private String mergeSummaries(String oldSummary, String newSummary) {
        if (oldSummary == null || oldSummary.isEmpty()) {
            return newSummary;
        }
        if (newSummary == null || newSummary.isEmpty()) {
            return oldSummary;
        }

        // 如果两个摘要都很短，直接合并
        if (oldSummary.length() + newSummary.length() < MAX_SUMMARY_LENGTH) {
            return oldSummary + "\n后续对话：" + newSummary;
        }

        // 否则使用AI合并
        try {
            String promptTemplate = """
                请将以下两段对话摘要合并成一段连贯的总结：
                
                历史摘要：{oldSummary}
                
                新对话摘要：{newSummary}
                
                要求：保留重要信息，语言简洁，不超过500字。
                合并结果：
                """;

            PromptTemplate template = new PromptTemplate(promptTemplate);
            Prompt prompt = template.create(Map.of(
                    "oldSummary", oldSummary,
                    "newSummary", newSummary
            ));

            String merged = chatModel.call(prompt).getResult().getOutput().getText();
            return merged.length() > MAX_SUMMARY_LENGTH ?
                    merged.substring(0, MAX_SUMMARY_LENGTH) + "..." : merged;

        } catch (Exception e) {
            log.error("合并摘要失败", e);
            return oldSummary + " | " + newSummary;
        }
    }

    /**
     * 创建摘要消息
     */
    private Message createSummaryMessage(String summary) {
        String content = """
            【对话历史摘要】
            %s
            
            --- 以下是最近的对话 ---
            """.formatted(summary);

        return new org.springframework.ai.chat.messages.SystemMessage(content);
    }

    /**
     * 获取压缩后的记忆（用于恢复）
     */
    public CompressedMemory getCompressedMemory(String sessionId) {
        return compressedMemoryCache.get(sessionId);
    }

    /**
     * 清除压缩缓存
     */
    public void clearCompressedMemory(String sessionId) {compressedMemoryCache.remove(sessionId);}

    /**
     * 压缩记忆数据类
     */
    @Data
    public static class CompressedMemory {
        private String summary;                    // 对话摘要
        private int lastCompressedIndex;           // 上次压缩位置
        private LocalDateTime compressTime;        // 压缩时间
        private int compressionCount = 0;          // 压缩次数

        public CompressedMemory() {
            this.summary = "";
            this.lastCompressedIndex = 0;
            this.compressTime = LocalDateTime.now();
            this.compressionCount = 0;
        }

        public void setSummary(String summary) {
            this.summary = summary;
            this.compressionCount++;
        }
    }
}
