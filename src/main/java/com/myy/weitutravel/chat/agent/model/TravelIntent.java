package com.myy.weitutravel.chat.agent.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析后的用户旅行意图
 */
public record TravelIntent(
        /** 意图类型 */
        IntentType type,
        /** 置信度 0.0-1.0 */
        double confidence,
        /** 提取的实体（城市名、日期、预算等） */
        Map<String, String> entities,
        /** 用户原始输入 */
        String originalQuery,
        /** 意图识别推理过程 */
        String reasoning
) {
    public static TravelIntent of(IntentType type, double confidence, String originalQuery, String reasoning) {
        return new TravelIntent(type, confidence, new HashMap<>(), originalQuery, reasoning);
    }

    public TravelIntent withEntity(String key, String value) {
        Map<String, String> copy = new HashMap<>(entities);
        copy.put(key, value);
        return new TravelIntent(type, confidence, copy, originalQuery, reasoning);
    }

    public TravelIntent withEntities(Map<String, String> entities) {
        Map<String, String> copy = new HashMap<>(this.entities);
        copy.putAll(entities);
        return new TravelIntent(type, confidence, copy, originalQuery, reasoning);
    }

    public String getEntity(String key) {
        return entities.get(key);
    }

    public boolean hasEntity(String key) {
        return entities.containsKey(key);
    }

    /** 常用实体 key */
    public static final String CITY = "city";
    public static final String DEPARTURE = "departure";
    public static final String DESTINATION = "destination";
    public static final String DATE = "date";
    public static final String CHECK_IN = "checkIn";
    public static final String CHECK_OUT = "checkOut";
    public static final String DAYS = "days";
    public static final String BUDGET = "budget";
    public static final String PREFERENCE = "preference";
    public static final String TRAVELERS = "travelers";
}
