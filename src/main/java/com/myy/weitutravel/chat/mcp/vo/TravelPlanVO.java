package com.myy.weitutravel.chat.mcp.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;
import java.util.List;

public class TravelPlanVO {

    /**
     * 旅游规划文档生成请求
     */
    public record TravelPlanRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("目的地城市，如：北京")
            String destination,

            @JsonProperty(required = true)
            @JsonPropertyDescription("旅行天数，如：3")
            int days,

            @JsonPropertyDescription("人均预算范围：如 2000-3000")
            String budget,

            @JsonPropertyDescription("旅行偏好：文化历史、自然风光、美食之旅、亲子游、蜜月游")
            String preference,

            @JsonPropertyDescription("出行人数")
            int travelers,

            @JsonPropertyDescription("航班信息JSON，来自机票查询结果")
            String flightInfo,

            @JsonPropertyDescription("酒店信息JSON，来自酒店查询结果")
            String hotelInfo
    ) {}

    /**
     * 旅游规划文档响应
     */
    public record TravelPlanResponse(
            String title,
            String destination,
            String duration,
            String budget,
            String overview,
            List<DayPlan> days,
            CostBreakdown costBreakdown,
            List<String> tips,
            String generatedAt
    ) {}

    /**
     * 每日行程
     */
    public record DayPlan(
            int day,
            String date,
            String morning,
            String afternoon,
            String evening,
            String hotel,
            String meals,
            List<String> estimatedCosts
    ) {}

    /**
     * 费用明细
     */
    public record CostBreakdown(
            BigDecimal flightCost,
            BigDecimal hotelCost,
            BigDecimal attractionsCost,
            BigDecimal mealsCost,
            BigDecimal transportCost,
            BigDecimal totalCost
    ) {}
}
