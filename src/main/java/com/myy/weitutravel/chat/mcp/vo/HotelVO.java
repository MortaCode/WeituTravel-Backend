package com.myy.weitutravel.chat.mcp.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;
import java.util.List;

public class HotelVO {

    /**
     * 酒店查询请求
     */
    public record HotelRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("目标城市名称，如：三亚、北京")
            String city,

            @JsonPropertyDescription("入住日期，格式：yyyy-MM-dd")
            String checkIn,

            @JsonPropertyDescription("离店日期，格式：yyyy-MM-dd")
            String checkOut,

            @JsonPropertyDescription("价格范围：budget-经济型, comfort-舒适型, luxury-豪华型")
            String priceLevel,

            @JsonPropertyDescription("最低评分，1-5")
            Integer minRating
    ) {}

    /**
     * 酒店查询响应
     */
    public record HotelResponse(
            String city,
            List<HotelItem> hotels
    ) {}

    /**
     * 单条酒店信息
     */
    public record HotelItem(
            String hotelId,
            String name,
            String address,
            String area,
            BigDecimal pricePerNight,
            Integer rating,
            Integer reviewCount,
            String highlights,
            List<String> amenities,
            String imageUrl,
            String distanceToDowntown,
            boolean breakfastIncluded,
            String checkInTime,
            String checkOutTime
    ) {}
}
