package com.myy.weitutravel.chat.controller;

import com.myy.weitutravel.chat.agent.AgentOrchestrator;
import com.myy.weitutravel.chat.vo.ChatMessageVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentOrchestrator agentOrchestrator;

    public ChatController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    /**
     * 智能体对话接口 —— 意图识别 + 任务拆分 + 任务编排 + 工具调用
     */
    @PostMapping("/ai")
    public ResponseEntity<Map<String, Object>> generation(@RequestBody ChatMessageVo messageVo) {
        log.info("Agent 开始处理: sessionId={}, model={}", messageVo.getSessionId(), messageVo.getModelName());

        AgentOrchestrator.AgentResponse response = agentOrchestrator.process(
                messageVo.getSessionId(),
                messageVo.getUserInput(),
                messageVo.getModelName()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", response.getContent());
        result.put("intent", response.getIntent().type().getName());
        result.put("confidence", String.format("%.0f%%", response.getIntent().confidence() * 100));
        result.put("taskCount", response.getPlan().getTasks().size());
        result.put("elapsedMs", response.getElapsedMs());
        result.put("reasoning", response.getIntent().reasoning());

        log.info("Agent 处理完成: {}", response.toSummary());
        return ResponseEntity.ok(result);
    }
}
