package com.myy.weitutravel.chat.controller;

import com.myy.weitutravel.chat.service.ModelSelectService;
import com.myy.weitutravel.chat.vo.ChatMessageVo;
import com.myy.weitutravel.chat.vo.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ModelSelectService modelSelectService;

    public ChatController(ModelSelectService modelSelectService) {
        this.modelSelectService = modelSelectService;
    }

    @PostMapping("/ai")
    public ResponseEntity<String> generation(@RequestBody ChatMessageVo messageVo) {
        ChatClient chatClient = modelSelectService.selectModel(ChatModel.fromString(messageVo.getModelName()));
        log.info("开始执行，大语言模型：{}", messageVo.getModelName());
        String result = chatClient
                .prompt()
                .user(messageVo.getUserInput())
                .advisors(advisorSpec -> {
                    advisorSpec.params(Map.ofEntries(
                            Map.entry("sessionId", messageVo.getSessionId()),
                            Map.entry("modelName", messageVo.getModelName())
                    ));
                })
                .call()
                .content();
        log.info("执行结束，输出内容：{}", result);
        return ResponseEntity.ok(result);
    }

}
