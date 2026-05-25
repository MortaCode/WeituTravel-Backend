package com.myy.weitutravel.chat.mcp.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;
import java.util.List;

public class FlightVO {

    /**
     * 机票查询请求
     */
    public record FlightRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("出发城市，如：北京、上海、广州")
            String departure,

            @JsonProperty(required = true)
            @JsonPropertyDescription("目的城市，如：三亚、成都、昆明")
            String arrival,

            @JsonProperty(required = true)
            @JsonPropertyDescription("出发日期，格式：yyyy-MM-dd")
            String date
    ) {}

    /**
     * 机票查询响应
     */
    public record FlightResponse(
            String departure,
            String arrival,
            String date,
            List<FlightItem> flights
    ) {}

    /**
     * 单条机票信息
     */
    public record FlightItem(
            String flightNo,
            String airline,
            String departTime,
            String arriveTime,
            String duration,
            BigDecimal price,
            Integer seatsAvailable,
            String cabinClass,
            String departAirport,
            String arriveAirport,
            boolean direct
    ) {}
}
