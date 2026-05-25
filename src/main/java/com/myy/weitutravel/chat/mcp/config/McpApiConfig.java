package com.myy.weitutravel.chat.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * MCP 工具 API 配置
 * 管理外部 API 的 RestClient 和配置参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.api")
public class McpApiConfig {

    /** 天气 API 配置 */
    private Weather weather = new Weather();

    /** 机票 API 配置 */
    private Flight flight = new Flight();

    /** 酒店 API 配置 */
    private Hotel hotel = new Hotel();

    /** 通用 HTTP 超时配置 */
    private Http http = new Http();

    @Data
    public static class Weather {
        /** Open-Meteo 基础 URL */
        private String baseUrl = "https://api.open-meteo.com";
        /** Open-Meteo 地理编码 URL */
        private String geoBaseUrl = "https://geocoding-api.open-meteo.com";
    }

    @Data
    public static class Flight {
        /** AviationStack API 基础 URL */
        private String baseUrl = "https://api.aviationstack.com/v1";
        /** API Key（从 https://aviationstack.com/signup/free 免费获取） */
        private String apiKey = "";
    }

    @Data
    public static class Hotel {
        /** Amadeus 认证 URL（测试环境） */
        private String authUrl = "https://test.api.amadeus.com/v1/security/oauth2/token";
        /** Amadeus 酒店 API 基础 URL */
        private String baseUrl = "https://test.api.amadeus.com/v1/reference-data/locations/hotels";
        /** Amadeus 酒店列表 API */
        private String hotelListUrl = "https://test.api.amadeus.com/v2/shopping/hotel-offers";
        /** API Key */
        private String apiKey = "";
        /** API Secret */
        private String apiSecret = "";
    }

    @Data
    public static class Http {
        private int connectTimeout = 10;
        private int readTimeout = 30;
    }

    @Bean
    public RestClient weatherRestClient() {
        return buildRestClient("");
    }

    @Bean
    public RestClient flightRestClient() {
        return buildRestClient(flight.baseUrl);
    }

    @Bean
    public RestClient hotelRestClient() {
        return buildRestClient(hotel.baseUrl);
    }

    @Bean
    public RestClient hotelAuthRestClient() {
        return buildRestClient("");
    }

    private RestClient buildRestClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(http.connectTimeout))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(http.readTimeout));
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }
}
