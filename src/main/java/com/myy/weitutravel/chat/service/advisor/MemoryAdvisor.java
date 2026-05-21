package com.myy.weitutravel.chat.service.advisor;

import cn.hutool.core.util.IdUtil;
import com.myy.weitutravel.chat.service.memory.ChatMemoryCompressService;
import com.myy.weitutravel.chat.service.memory.ChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MemoryAdvisor implements CallAdvisor, StreamAdvisor {

    private final ChatMemoryCompressService chatMemoryCompressService;
    private final ChatMemoryService chatMemoryService;

    // 默认上下文窗口大小
    private static final int DEFAULT_CONTEXT_WINDOW = 10;
    private int contextWindowSize = DEFAULT_CONTEXT_WINDOW;

    public MemoryAdvisor(ChatMemoryCompressService chatMemoryCompressService, ChatMemoryService chatMemoryService) {
        this.chatMemoryCompressService = chatMemoryCompressService;
        this.chatMemoryService = chatMemoryService;
    }


    /**
     * 设置上下文窗口大小
     */
    public MemoryAdvisor withContextWindowSize(int size) {
        this.contextWindowSize = size;
        return this;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.debug("顾问开启");

        // 1. 获取或生成会话ID
        String sessionId = extractSessionId(chatClientRequest);
        if (!StringUtils.hasText(sessionId)) {
            sessionId = IdUtil.objectId();  // 生成新会话ID
            chatClientRequest = setSessionId(chatClientRequest, sessionId);  // 设置到请求中
            log.debug("首次会话，生成新会话ID: {}", sessionId);
        }

        // 2. 加载历史消息
        List<Message> historyMessages = loadHistoryMessages(sessionId);
        log.debug("加载会话{}的历史消息：{}", sessionId, historyMessages.size());

        // 3. 构建包含历史消息的新Prompt
        ChatClientRequest enhancedRequest = enhanceRequestWithHistory(chatClientRequest, historyMessages);

        // 4. 执行调用链
        ChatClientResponse response = callAdvisorChain.nextCall(enhancedRequest);

        // 5. 保存本次对话到历史
        if (response != null && response.chatResponse() != null) {
            saveConversation(sessionId, chatClientRequest, response);
        }

        log.debug("顾问关闭");
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.debug("顾问开启");

        // 1. 获取会话ID
        String sessionId = extractSessionId(chatClientRequest);
        if (!StringUtils.hasText(sessionId)) {
            sessionId = IdUtil.objectId();  // 生成新会话ID
            chatClientRequest = setSessionId(chatClientRequest, sessionId);  // 设置到请求中
            log.debug("首次会话，生成新会话ID: {}", sessionId);
        }

        // 2. 加载历史消息
        List<Message> historyMessages = loadHistoryMessages(sessionId);
        log.debug("加载会话{}的历史消息：{}", sessionId, historyMessages.size());

        // 3. 构建包含历史消息的新Prompt
        ChatClientRequest enhancedRequest = enhanceRequestWithHistory(chatClientRequest, historyMessages);

        // 4. 执行流式调用链
        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(enhancedRequest);

        // 5. 收集流式响应并保存完整对话
        return saveStreamConversation(sessionId, chatClientRequest, responseFlux);
    }

    @Override
    public String getName() {
        return "chatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 100; // 较低优先级，确保在其他顾问之后执行
    }

    /**
     * 从请求中提取会话ID
     */
    private String extractSessionId(ChatClientRequest request) {
        // 尝试从userParams获取sessionId
        if (request.context() != null && request.context().containsKey("sessionId")) {
            Object sessionId = request.context().get("sessionId");
            return sessionId != null ? sessionId.toString() : null;
        }
        return null;
    }

    /**
     * 设置会话ID
     * @param chatClientRequest
     * @param sessionId
     * @return
     */
    private ChatClientRequest setSessionId(ChatClientRequest chatClientRequest, String sessionId) {
        chatClientRequest.context().put("sessionId", sessionId);
        return chatClientRequest;
    }

    /**
     * 加载历史消息
     */
    private List<Message> loadHistoryMessages(String sessionId) {
        try {
            //List<Message> historyEntities = chatMemoryService.getMemory(sessionId, contextWindowSize);
            List<Message> historyEntities = chatMemoryCompressService.getMemoryWithCompress(sessionId);
            return historyEntities;
        } catch (Exception e) {
            log.error("会话{}获取历史消息异常", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 增强请求，添加历史消息
     */
    private ChatClientRequest enhanceRequestWithHistory(ChatClientRequest request, List<Message> historyMessages) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return request;
        }

        // 获取原始Prompt
        Prompt originalPrompt = request.prompt();
        List<Message> originalMessages = originalPrompt.getInstructions();

        // 合并历史消息和当前消息
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(historyMessages);
        allMessages.addAll(originalMessages);

        // 创建新的Prompt
        Prompt enhancedPrompt = new Prompt(allMessages, originalPrompt.getOptions());

        // 创建新的ChatClientRequest
        return ChatClientRequest.builder()
                .prompt(enhancedPrompt)
                .build();
    }

    /**
     * 保存对话
     */
    private void saveConversation(String sessionId, ChatClientRequest request, ChatClientResponse response) {
        try {
            // 获取用户消息
            String userMessage = extractUserMessage(request);

            // 获取助手回复
            String assistantMessage = extractAssistantMessage(response);

            // 保存到历史
            if (userMessage != null && assistantMessage != null) {
                // 创建用户消息和助手消息
                List<Message> messages = new ArrayList<>();
                messages.add(new UserMessage(userMessage));
                messages.add(new AssistantMessage(assistantMessage.toString()));
                chatMemoryService.saveMemory(sessionId, messages);
                log.debug("保存聊天信息，会话{}", sessionId);
            }
        } catch (Exception e) {
            log.error("保存聊天信息出错，会话{}", sessionId, e);
        }
    }

    /**
     * 保存流式对话
     */
    private Flux<ChatClientResponse> saveStreamConversation(String sessionId, ChatClientRequest request,
                                                            Flux<ChatClientResponse> responseFlux) {
        StringBuilder fullResponse = new StringBuilder();
        String userMessage = extractUserMessage(request);

        return responseFlux
                .doOnNext(response -> {
                    // 累积响应内容
                    if (response != null && response.chatResponse() != null) {
                        String content = response.chatResponse().getResult().getOutput().getText();
                        if (content != null) {
                            fullResponse.append(content);
                        }
                    }
                })
                .doOnComplete(() -> {
                    // 流式响应完成后保存完整对话
                    try {
                        if (userMessage != null && fullResponse.length() > 0) {
                            // 创建用户消息和助手消息
                            List<Message> messages = new ArrayList<>();
                            messages.add(new UserMessage(userMessage));
                            messages.add(new AssistantMessage(fullResponse.toString()));
                            chatMemoryService.saveMemory(sessionId, messages);
                            log.debug("保存聊天信息，会话{}", sessionId);
                        }
                    } catch (Exception e) {
                        log.error("保存聊天信息出错，会话{}", sessionId, e);
                    }
                });
    }
    /**
     * 从请求中提取用户消息
     */
    private String extractUserMessage(ChatClientRequest request) {
        if (request.prompt() == null) {
            return null;
        }

        List<Message> messages = request.prompt().getInstructions();
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 获取最后一条用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return msg.getText();
            }
        }

        return null;
    }

    /**
     * 从响应中提取助手消息
     */
    private String extractAssistantMessage(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return null;
        }

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return null;
        }

        return chatResponse.getResult().getOutput().getText();
    }
}