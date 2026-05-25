package com.myy.weitutravel.chat.mcp.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class WeatherVO {

    /**
     * 天气查询请求
     */
    public record WeatherRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("查询城市名称，如：三亚、北京、成都")
            String city,

            @JsonProperty(required = true)
            @JsonPropertyDescription("查询日期，格式：yyyy-MM-dd，最多查询未来7天")
            String date
    ) {}

    /**
     * 天气查询响应
     */
    public record WeatherResponse(
            String city,
            String date,
            String dayWeather,
            String nightWeather,
            String dayTemp,
            String nightTemp,
            String humidity,
            String wind,
            String uvIndex,
            String airQuality,
            String sunrise,
            String sunset,
            String travelAdvice,
            String clothingAdvice
    ) {}
}
