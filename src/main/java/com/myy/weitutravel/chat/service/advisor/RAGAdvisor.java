//package com.myy.weitutravel.chat.service.advisor;
//
//import com.myy.weitutravel.chat.service.retriever.HierarchicalRetrieverService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClientRequest;
//import org.springframework.ai.chat.client.ChatClientResponse;
//import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
//import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
//import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
//import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.ai.chat.messages.SystemMessage;
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import reactor.core.publisher.Flux;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RAGAdvisor implements CallAdvisor, StreamAdvisor {
//
//    private final HierarchicalRetrieverService hierarchicalRetrieverService;
//
//    // RAG 系统提示词模板
//    private static final String RAG_SYSTEM_PROMPT = """
//        你是一个专业的旅行规划助手。请基于以下知识库内容回答用户问题。
//
//        知识库内容：
//        {context}
//
//        注意事项：
//        1. 如果知识库中没有相关信息，请明确告知用户，并建议用户查看官方渠道或咨询当地旅游机构
//        2. 回答要准确、实用，避免编造景点信息、门票价格、开放时间等
//        3. 涉及门票购买、优惠券领取、行程安排时，必须强调以平台实时查询结果为准
//        4. 保持回答简洁清晰，可以适当给出旅行小贴士
//        5. 对于景点推荐，优先使用知识库中的攻略信息；对于实时信息（库存、天气），引导用户使用查询工具
//        """;
//
//    @Override
//    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
//        log.debug("RAG顾问开始处理");
//
//        // 1. 提取用户问题
//        String userQuery = extractUserQuery(request);
//        if (!StringUtils.hasText(userQuery)) {
//            return chain.nextCall(request);
//        }
//
//        // 2. 从知识库检索相关内容
//        String relevantContext = hierarchicalRetrieverService.retrieveRelevantContext(userQuery, 5);
//
//        // 3. 构建增强的Prompt
//        ChatClientRequest enhancedRequest = enhanceRequestWithContext(request, relevantContext);
//
//        // 4. 继续调用链
//        return chain.nextCall(enhancedRequest);
//    }
//
//    @Override
//    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
//        log.debug("RAG顾问流式处理开始");
//
//        String userQuery = extractUserQuery(request);
//        if (!StringUtils.hasText(userQuery)) {
//            return chain.nextStream(request);
//        }
//
//        String relevantContext = hierarchicalRetrieverService.retrieveRelevantContext(userQuery, 5);
//        ChatClientRequest enhancedRequest = enhanceRequestWithContext(request, relevantContext);
//
//        return chain.nextStream(enhancedRequest);
//    }
//
//    private ChatClientRequest enhanceRequestWithContext(ChatClientRequest request, String context) {
//        // 如果没有检索到相关内容，直接返回原请求
//        if (!StringUtils.hasText(context)) {
//            log.debug("未检索到相关知识，使用原请求");
//            return request;
//        }
//
//        // 构建包含知识库的SystemMessage
//        String systemPrompt = RAG_SYSTEM_PROMPT.replace("{context}", context);
//        SystemMessage ragSystemMessage = new SystemMessage(systemPrompt);
//
//        // 获取原始消息
//        Prompt originalPrompt = request.prompt();
//        List<Message> originalMessages = originalPrompt.getInstructions();
//
//        // 构建新消息列表：RAG SystemMessage + 原始消息（移除原有的SystemMessage）
//        List<Message> newMessages = new ArrayList<>();
//        newMessages.add(ragSystemMessage);
//
//        for (Message msg : originalMessages) {
//            if (!(msg instanceof SystemMessage)) {
//                newMessages.add(msg);
//            }
//        }
//
//        // 创建新的Prompt
//        Prompt enhancedPrompt = new Prompt(newMessages, originalPrompt.getOptions());
//
//        return ChatClientRequest.builder()
//                .prompt(enhancedPrompt)
//                .context(request.context())
//                .build();
//    }
//
//    /**
//     * ChatClientRequest  -->  prompt  -->  getInstructions()/List<Message>
//     * UserMessage{content='" + var10000 + "', metadata=" + String.valueOf(this.metadata) + ", messageType=" + String.valueOf(this.messageType) + "}
//     * messageType -->  USER、ASSISTANT、SYSTEM、TOOL  public static MessageType fromValue(String value) {}
//     * @param request
//     * @return
//     */
//    private String extractUserQuery(ChatClientRequest request) {
//        if (request.prompt() == null) {
//            return null;
//        }
//
//        List<Message> messages = request.prompt().getInstructions();
//        if (messages == null || messages.isEmpty()) {
//            return null;
//        }
//
//        // 获取最后一条用户消息
//        for (int i = messages.size() - 1; i >= 0; i--) {
//            Message msg = messages.get(i);
//            if (msg instanceof UserMessage) {
//                return msg.getText();
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public String getName() {
//        return "ragAdvisor";
//    }
//
//    @Override
//    public int getOrder() {
//        return 50; // 在ChatMemoryAdvisor之前执行（order值越小优先级越高）
//    }
//}
