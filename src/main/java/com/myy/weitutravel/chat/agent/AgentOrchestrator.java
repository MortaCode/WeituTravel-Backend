package com.myy.weitutravel.chat.agent;

import com.myy.weitutravel.chat.agent.model.AgentPlan;
import com.myy.weitutravel.chat.agent.model.TravelIntent;
import com.myy.weitutravel.chat.service.ModelSelectService;
import com.myy.weitutravel.chat.vo.ChatModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能体编排器 —— 核心入口
 *
 * 流程：用户输入 → 意图识别 → 任务规划 → 注入执行计划 → ChatClient 执行 → 聚合响应
 */
@Slf4j
@Service
public class AgentOrchestrator {

    private final IntentRecognitionService intentService;
    private final TaskPlanningService taskPlanningService;
    private final ModelSelectService modelSelectService;
    private final AgentAdvisor agentAdvisor;

    public AgentOrchestrator(IntentRecognitionService intentService,
                             TaskPlanningService taskPlanningService,
                             ModelSelectService modelSelectService,
                             AgentAdvisor agentAdvisor) {
        this.intentService = intentService;
        this.taskPlanningService = taskPlanningService;
        this.modelSelectService = modelSelectService;
        this.agentAdvisor = agentAdvisor;
    }

    /**
     * Agent 完整执行流程
     */
    public AgentResponse process(String sessionId, String userInput, String modelName) {
        long startTime = System.currentTimeMillis();
        log.info("==============================");
        log.info("Agent 开始处理: model={}, input={}", modelName, userInput);

        // === Phase 1: 意图识别 ===
        TravelIntent intent = intentService.recognize(userInput);

        // === Phase 2: 任务规划 ===
        AgentPlan plan = taskPlanningService.plan(intent);

        // === Phase 3: 构建增强 Prompt 并执行 ===
        String enhancedInput = buildEnhancedUserInput(userInput, intent, plan);

        ChatClient chatClient = modelSelectService.selectModel(ChatModel.fromString(modelName));

        String llmResult;
        try {
            llmResult = chatClient
                    .prompt()
                    .user(enhancedInput)
                    .advisors(advisorSpec -> {
                        advisorSpec.param("sessionId", sessionId);
                        advisorSpec.param("modelName", modelName);
                        advisorSpec.param("agentPlan", plan);  // 传递给 AgentAdvisor
                    })
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("ChatClient 调用失败", e);
            llmResult = "抱歉，智能体执行过程中出现异常：" + e.getMessage();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Agent 处理完成: intent={}, tasks={}, time={}ms",
                intent.type().getName(), plan.getTasks().size(), elapsed);
        log.info("==============================");

        return new AgentResponse(intent, plan, llmResult, elapsed);
    }

    /**
     * 构建增强的用户输入，嵌入意图识别和任务规划结果
     */
    private String buildEnhancedUserInput(String originalInput, TravelIntent intent, AgentPlan plan) {
        StringBuilder sb = new StringBuilder();

        sb.append("【Agent 分析结果】\n");
        sb.append("意图类型：").append(intent.type().getName()).append("\n");
        sb.append("置信度：").append(String.format("%.0f%%", intent.confidence() * 100)).append("\n");
        sb.append("识别推理：").append(intent.reasoning()).append("\n");

        if (!intent.entities().isEmpty()) {
            sb.append("提取参数：");
            intent.entities().forEach((k, v) -> sb.append(k).append("=").append(v).append(", "));
            sb.setLength(sb.length() - 2);  // 移除末尾逗号
            sb.append("\n");
        }

        if (!plan.getTasks().isEmpty()) {
            sb.append("任务计划：共 ").append(plan.getTasks().size()).append(" 个步骤\n");
        }

        sb.append("\n【用户原始提问】\n");
        sb.append(originalInput);

        return sb.toString();
    }

    /**
     * Agent 响应结果
     */
    @Data
    @AllArgsConstructor
    public static class AgentResponse {
        /** 识别出的意图 */
        private TravelIntent intent;
        /** 任务计划 */
        private AgentPlan plan;
        /** LLM 最终回复 */
        private String content;
        /** 总耗时（毫秒） */
        private long elapsedMs;
        /** 时间戳 */
        private String timestamp = LocalDateTime.now().toString();

        public AgentResponse(TravelIntent intent, AgentPlan plan, String content, long elapsedMs) {
            this.intent = intent;
            this.plan = plan;
            this.content = content;
            this.elapsedMs = elapsedMs;
        }

        public String toSummary() {
            return String.format("[%s] 意图=%s(%.0f%%) 任务数=%d 耗时=%dms",
                    timestamp, intent.type().getName(), intent.confidence() * 100,
                    plan.getTasks().size(), elapsedMs);
        }
    }
}
