package com.myy.weitutravel.chat.service.memory;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.chat.entity.ChatMessage;
import com.myy.weitutravel.chat.entity.ChatSession;
import com.myy.weitutravel.chat.mapper.ChatMessageMapper;
import com.myy.weitutravel.chat.mapper.ChatSessionMapper;
import com.myy.weitutravel.chat.service.ChatMessageService;
import com.myy.weitutravel.chat.service.ChatSessionService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 聊天记忆
 */
@Service
@AllArgsConstructor
public class ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatMessageService messageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KryoSerializer kryoSerializer;

    private final Map<String, List<Message>> localCache = new ConcurrentHashMap<>();
    private static final String REDIS_KEY_PREFIX = "chat:memory:";
    private static final int REDIS_TTL_HOURS = 24;
    private static final int LOCAL_CACHE_MAX_SIZE = 1000;

    /**
     * 保存会话记忆
     */
    @Transactional
    public void saveMemory(String sessionId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            // 1. 更新本地缓存
            updateLocalCache(sessionId, messages);

            // 2. 更新Redis缓存
            updateRedisCache(sessionId, messages);

            // 3. 异步持久化到MySQL
            persistToDatabase(sessionId, messages);

        } catch (Exception e) {
            log.error("保存会话记忆失败, sessionId: {}", sessionId);
            throw new RuntimeException("保存会话记忆失败", e);
        }
    }

    /**
     * 获取会话记忆
     */
    public List<Message> getMemory(String sessionId) {
        // 1. 先从本地缓存获取
        List<Message> messages = localCache.get(sessionId);
        if (messages != null && !messages.isEmpty()) {
            log.debug("从本地缓存获取会话记忆, sessionId: {}, 消息数: {}", sessionId, messages.size());
            return messages;
        }

        // 2. 从Redis获取
        messages = getFromRedis(sessionId);
        if (messages != null && !messages.isEmpty()) {
            log.debug("从Redis获取会话记忆, sessionId: {}, 消息数: {}", sessionId, messages.size());
            // 更新本地缓存
            updateLocalCache(sessionId, messages);
            return messages;
        }

        // 3. 从MySQL恢复
        messages = getFromDatabase(sessionId);
        if (messages != null && !messages.isEmpty()) {
            log.debug("从MySQL恢复会话记忆, sessionId: {}, 消息数: {}", sessionId, messages.size());
            // 恢复后更新缓存
            updateLocalCache(sessionId, messages);
            updateRedisCache(sessionId, messages);
            return messages;
        }

        return new ArrayList<>();
    }

    /**
     * 更新本地缓存
     */
    public void updateLocalCache(String sessionId, List<Message> messages) {
        // 限制本地缓存大小
        if (localCache.size() >= LOCAL_CACHE_MAX_SIZE) {
            // 移除最早的缓存
            String oldestKey = localCache.keySet().iterator().next();
            localCache.remove(oldestKey);
        }
        localCache.put(sessionId, new ArrayList<>(messages));
    }

    /**
     * 更新Redis缓存
     */
    public void updateRedisCache(String sessionId, List<Message> messages) {
        String key = REDIS_KEY_PREFIX + sessionId;
        byte[] serializedData = kryoSerializer.serialize(messages);
        redisTemplate.opsForValue().set(key, serializedData, Duration.ofHours(REDIS_TTL_HOURS));
    }

    /**
     * 从Redis获取记忆
     */
    @SuppressWarnings("unchecked")
    private List<Message> getFromRedis(String sessionId) {
        String key = REDIS_KEY_PREFIX + sessionId;
        Object data = redisTemplate.opsForValue().get(key);
        if (data != null) {
            try {
                return (List<Message>) kryoSerializer.deserialize((byte[]) data);
            } catch (Exception e) {
                log.error("Redis反序列化失败, sessionId: {}", sessionId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * 从数据库获取记忆
     */
    private List<Message> getFromDatabase(String sessionId) {
        // 检查会话是否存在
        ChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .eq(ChatSession::getDelFlag, 0)
        );

        if (session == null) {
            return null;
        }

        // 获取所有消息
        List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );

        // 转换为Spring AI Message对象
        return messages.stream()
                .map(this::convertToAiMessage)
                .collect(Collectors.toList());
    }

    /**
     * 持久化到数据库
     */
    private void persistToDatabase(String sessionId, List<Message> messages) {
        // 检查会话是否存在，不存在则创建
        ChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
        );

        if (session == null) {
            session = new ChatSession();
            session.setId(sessionId);
            session.setUserId(getUserIdFromSession(sessionId));
            session.setModelName("modelname");//ToDo
            session.setDelFlag(0);
            session.setMessageCount(messages.size());
            sessionMapper.insert(session);
        } else {
            session.setMessageCount(messages.size());
            sessionMapper.updateById(session);
        }

        // 保存消息（使用批量插入）
        List<ChatMessage> messageEntities = messages.stream()
                .map(msg -> convertToEntity(sessionId, msg))
                .collect(Collectors.toList());

        // 先删除旧消息，再插入新消息
//        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
//                .eq(ChatMessage::getSessionId, sessionId));
        if (!messageEntities.isEmpty()) {
            messageService.saveBatch(messageEntities);
        }
    }

    /**
     * 转换AI Message为数据库实体
     */
    private ChatMessage convertToEntity(String sessionId, Message message) {
        ChatMessage entity = new ChatMessage();
        entity.setId(IdUtil.objectId());
        entity.setSessionId(sessionId);
        entity.setRole(message.getMessageType().name().toLowerCase());
        entity.setContent(message.getText());
        entity.setMessageType("text");
        return entity;
    }

    /**
     * 转换数据库实体为AI Message
     */
    private Message convertToAiMessage(ChatMessage entity) {
        if ("user".equalsIgnoreCase(entity.getRole())) {
            return new UserMessage(entity.getContent());
        } else if ("assistant".equals(entity.getRole())) {
            return new AssistantMessage(entity.getContent());
        }
        throw new IllegalArgumentException("未知角色: " + entity.getMessageType());
    }

    /**
     * 从sessionId中获取userId（根据实际业务逻辑实现）
     */
    private String getUserIdFromSession(String sessionId) {
        // 目前没有用户信息，可以根据sessionId解析出userId，或者从上下文获取
        return "default_user";
    }


    /**
     * 清除会话缓存
     */
    public void clearCache(String sessionId) {
        if (!StringUtils.hasText(sessionId)){return;}
        // 清除本地缓存
        localCache.remove(sessionId);

        // 清除Redis
        String key = REDIS_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);

        log.info("清除会话缓存成功, sessionId: {}", sessionId);
    }

    /**
     * 清除会话记忆
     */
    public void clearMemory(String sessionId) {
        // 清除本地缓存
        localCache.remove(sessionId);

        // 清除Redis
        String key = REDIS_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);

        // 更新数据库状态
        ChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
        );
        if (session != null) {
            session.setDelFlag(1);
            sessionMapper.updateById(session);
        }

        log.info("清除会话记忆成功, sessionId: {}", sessionId);
    }
}
