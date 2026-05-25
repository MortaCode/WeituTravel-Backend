package com.myy.weitutravel.chat.agent;

import com.myy.weitutravel.chat.agent.model.AgentPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行顾问 —— 注入 ReAct (Reasoning + Acting) 模式提示词
 * 引导 LLM 按照 Thought → Action → Observation 流程逐步完成任务规划
 */
@Slf4j
@Component
public class AgentAdvisor implements CallAdvisor {

    private static final String REACT_PROMPT = """
            ## Agent 工作模式：ReAct (思考-行动-观察)

            你需要按照以下模式工作：

            **思考（Thought）**：分析当前需要做什么，需要什么信息。
            **行动（Action）**：调用合适的工具获取数据。
            **观察（Observation）**：分析工具返回的结果。
            **下一步思考**：根据观察结果决定下一步行动。
            **最终回答**：汇总所有信息，给出完整答复。

            ### 重要规则
            1. 每次只调用一个工具，等待结果后再决定下一步
            2. 严禁编造数据，必须通过工具获取真实信息
            3. 如果一个工具返回错误或无结果，尝试调整参数重试，最多重试2次
            4. 完成所有步骤后，将各工具返回的信息整合成一份完整的旅行规划回复
            5. 回复中使用具体的航班号、价格、酒店名称等真实数据
            """;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.debug("AgentAdvisor 注入 ReAct 提示词");

        // 1. 检查上下文中的 AgentPlan
        AgentPlan plan = null;
        if (request.context() != null && request.context().containsKey("agentPlan")) {
            Object planObj = request.context().get("agentPlan");
            if (planObj instanceof AgentPlan) {
                plan = (AgentPlan) planObj;
            }
        }

        // 2. 构建增强的 Prompt
        Prompt originalPrompt = request.prompt();
        List<Message> originalMessages = originalPrompt.getInstructions();

        List<Message> newMessages = new ArrayList<>();

        // 添加 ReAct 指令作为 SystemMessage
        newMessages.add(new SystemMessage(REACT_PROMPT));

        // 如果有执行计划，添加计划指令
        if (plan != null && !plan.getTasks().isEmpty()) {
            newMessages.add(new SystemMessage(plan.toPromptInstructions()));
        }

        // 保留原始消息（排除了原有的 SystemMessages，因为我们已经替换了）
        for (Message msg : originalMessages) {
            if (!(msg instanceof SystemMessage)) {
                newMessages.add(msg);
            }
        }

        // 3. 创建新请求
        Prompt enhancedPrompt = new Prompt(newMessages, originalPrompt.getOptions());
        ChatClientRequest enhancedRequest = ChatClientRequest.builder()
                .prompt(enhancedPrompt)
                .context(request.context())
                .build();

        // 4. 继续执行调用链
        return chain.nextCall(enhancedRequest);
    }

    @Override
    public String getName() {
        return "agentAdvisor";
    }

    @Override
    public int getOrder() {
        return 60; // 在 MemoryAdvisor(100) 之前执行
    }
}
