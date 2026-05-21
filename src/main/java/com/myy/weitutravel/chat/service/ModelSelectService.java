package com.myy.weitutravel.chat.service;

import com.myy.weitutravel.chat.vo.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ModelSelectService {
    private final ChatClient deepSeekClient;
    private final ChatClient qwenChatClient;

    public ModelSelectService(@Qualifier("deepseekChatClient") ChatClient deepSeekClient,
                          @Qualifier("qwenChatClient") ChatClient qwenChatClient) {
        this.deepSeekClient = deepSeekClient;
        this.qwenChatClient = qwenChatClient;
    }

    //利用Fall-through
    public ChatClient selectModel(ChatModel chatModel) {
        switch (chatModel) {
            case QWEN:
                return qwenChatClient;
            case DEEPSEEK:
            default:
                return deepSeekClient;
        }
    }
}
