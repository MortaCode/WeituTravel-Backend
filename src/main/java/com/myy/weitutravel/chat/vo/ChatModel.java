package com.myy.weitutravel.chat.vo;

import lombok.Getter;

@Getter
public enum ChatModel {
    DEEPSEEK("deepseek", "深度搜索模型"),
    QWEN("qwen", "千问模型");

    private String modelName;
    private String modelDes;

    ChatModel(String modelName, String modelDes) {
        this.modelName = modelName;
        this.modelDes = modelDes;
    }

    public String getModelName(){return this.modelName;}
    public String getModelDes(){return this.modelDes;}

    public static ChatModel fromString(String modelName){
        for (ChatModel model : ChatModel.values()){
            if (model.getModelName().equalsIgnoreCase(modelName)){
                return model;
            }
        }
        return DEEPSEEK;
    }
}
