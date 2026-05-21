package com.myy.weitutravel.chat.controller;

import com.myy.weitutravel.chat.service.memory.ChatMemoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping("chat/memory")
public class ChatMemoryController {

    private final ChatMemoryService chatMemoryService;

    /**
     * 清除会话缓存
     * @param sessionId
     * @return
     */
    @GetMapping("clear/cache")
    public ResponseEntity<String> clearCache(@RequestParam String sessionId){
        chatMemoryService.clearCache(sessionId);
        return ResponseEntity.ok("清理成功");
    }
}
