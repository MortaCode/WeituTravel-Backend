package com.myy.weitutravel.chat.agent.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户意图类型枚举
 */
@Getter
@AllArgsConstructor
public enum IntentType {

    WEATHER_QUERY("天气查询", "用户想查询某个城市的天气情况", false),
    FLIGHT_QUERY("机票查询", "用户想查询航班信息", false),
    HOTEL_QUERY("酒店查询", "用户想查询酒店信息", false),
    TRAVEL_PLAN("旅行规划", "用户想规划一次完整旅行，需要组合多个工具", true),
    ATTRACTION_QUERY("景点查询", "用户想了解景点信息", false),
    GENERAL_CHAT("一般对话", "用户进行一般性对话或咨询", false);

    /** 意图中文名 */
    private final String name;
    /** 意图描述 */
    private final String description;
    /** 是否为复合意图（需要多工具协作） */
    private final boolean composite;
}
