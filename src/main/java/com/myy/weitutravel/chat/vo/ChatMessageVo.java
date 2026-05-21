package com.myy.weitutravel.chat.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class ChatMessageVo {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户输入
     */
    @NotBlank(message = "用户输入不能为空")
    private String userInput;

    /**
     * 选中模型
     */
    private String modelName;

    public String getSessionId() {return sessionId;}

    public String getUserInput() {return userInput;}

    public String getModelName() {
        return StringUtils.hasText(this.modelName) ? this.modelName : "deepseek";
    }


}
