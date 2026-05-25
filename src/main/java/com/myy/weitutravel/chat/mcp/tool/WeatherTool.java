package com.myy.weitutravel.chat.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.myy.weitutravel.chat.mcp.vo.WeatherVO.WeatherRequest;
import com.myy.weitutravel.chat.mcp.vo.WeatherVO.WeatherResponse;
import com.myy.weitutravel.chat.mcp.config.McpApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 天气查询 MCP 工具 - 调用 Open-Meteo 免费天气 API
 * 无需 API Key，完全免费开放使用
 */
@Slf4j
@Component("weatherQuery")
public class WeatherTool {

    private final RestClient restClient;
    private final McpApiConfig config;

    public WeatherTool(RestClient weatherRestClient, McpApiConfig config) {
        this.restClient = weatherRestClient;
        this.config = config;
    }

    // WMO 天气代码 → 中文描述映射
    private static final Map<Integer, String> WMO_CODES = Map.ofEntries(
            Map.entry(0, "晴天"), Map.entry(1, "大部晴朗"), Map.entry(2, "多云"),
            Map.entry(3, "阴天"), Map.entry(45, "有雾"), Map.entry(48, "雾凇"),
            Map.entry(51, "小毛毛雨"), Map.entry(53, "毛毛雨"), Map.entry(55, "大毛毛雨"),
            Map.entry(56, "冻毛毛雨"), Map.entry(57, "大冻毛毛雨"),
            Map.entry(61, "小雨"), Map.entry(63, "中雨"), Map.entry(65, "大雨"),
            Map.entry(66, "冻雨"), Map.entry(67, "大冻雨"),
            Map.entry(71, "小雪"), Map.entry(73, "中雪"), Map.entry(75, "大雪"),
            Map.entry(77, "雪粒"), Map.entry(80, "阵雨"), Map.entry(81, "大阵雨"),
            Map.entry(82, "暴阵雨"), Map.entry(85, "小阵雪"), Map.entry(86, "大阵雪"),
            Map.entry(95, "雷暴"), Map.entry(96, "雷暴伴小冰雹"), Map.entry(99, "雷暴伴大冰雹")
    );

    @Tool(description = "查询指定城市在指定日期的真实天气信息，包括温度、天气状况、湿度、风力、UV指数等")
    public WeatherResponse queryWeather(@ToolParam(description = "天气查询条件") WeatherRequest request) {
        log.info("天气查询(Open-Meteo): 城市={}, 日期={}", request.city(), request.date());

        try {
            // Step 1: 地理编码 - 城市名 → 经纬度
            double[] coords = geocodeCity(request.city());
            if (coords == null) {
                return createErrorResponse(request.city(), request.date(), "未找到城市坐标");
            }

            // Step 2: 获取天气数据
            ForecastData forecast = fetchWeather(coords[0], coords[1], request.date());

            if (forecast == null) {
                return createErrorResponse(request.city(), request.date(), "天气数据获取失败");
            }

            int dayIndex = calculateDayIndex(request.date(), forecast.daily.time);

            String dayWeather = getWeatherDesc(forecast.daily.weatherCode.get(Math.min(dayIndex, forecast.daily.weatherCode.size() - 1)));
            String nightWeather = getWeatherDesc(forecast.daily.weatherCode.get(Math.min(dayIndex + 1 < forecast.daily.weatherCode.size() ? dayIndex + 1 : dayIndex, forecast.daily.weatherCode.size() - 1)));

            return new WeatherResponse(
                    request.city(), request.date(),
                    dayWeather, nightWeather,
                    formatTemp(forecast.daily.tempMax.get(Math.min(dayIndex, forecast.daily.tempMax.size() - 1))),
                    formatTemp(forecast.daily.tempMin.get(Math.min(dayIndex, forecast.daily.tempMin.size() - 1))),
                    forecast.current != null ? forecast.current.humidity + "%" : "N/A",
                    forecast.current != null ? forecast.current.windSpeed + " km/h" : "N/A",
                    formatUv(forecast.daily.uvIndex.get(Math.min(dayIndex, forecast.daily.uvIndex.size() - 1))),
                    "N/A",
                    forecast.daily.sunrise.get(Math.min(dayIndex, forecast.daily.sunrise.size() - 1)),
                    forecast.daily.sunset.get(Math.min(dayIndex, forecast.daily.sunset.size() - 1)),
                    generateTravelAdvice(request.city(), dayWeather, forecast.daily.tempMax.get(Math.min(dayIndex, forecast.daily.tempMax.size() - 1))),
                    generateClothingAdvice(forecast.daily.tempMax.get(Math.min(dayIndex, forecast.daily.tempMax.size() - 1)), dayWeather)
            );
        } catch (Exception e) {
            log.error("天气查询异常: 城市={}", request.city(), e);
            return createErrorResponse(request.city(), request.date(), "查询异常: " + e.getMessage());
        }
    }

    /**
     * Open-Meteo 地理编码 API
     */
    private double[] geocodeCity(String city) {
        try {
            String geoUrl = config.getWeather().getGeoBaseUrl() + "/v1/search?name={city}&count=1&language=zh";
            JsonNode response = restClient.get()
                    .uri(geoUrl, city)
                    .retrieve()
                    .onStatus(s -> s != HttpStatus.OK, (req, res) -> {
                        log.warn("地理编码请求失败: status={}", res.getStatusCode());
                    })
                    .body(JsonNode.class);

            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode result = response.get("results").get(0);
                return new double[]{result.get("latitude").asDouble(), result.get("longitude").asDouble()};
            }
            log.warn("地理编码未找到城市: {}", city);
            return null;
        } catch (Exception e) {
            log.error("地理编码异常: {}", city, e);
            return null;
        }
    }

    /**
     * Open-Meteo 天气预报 API
     */
    private ForecastData fetchWeather(double lat, double lon, String targetDate) {
        try {
            String weatherUrl = config.getWeather().getBaseUrl() + "/v1/forecast"
                    + "?latitude={lat}&longitude={lon}"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,uv_index_max,sunrise,sunset"
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&timezone=auto&forecast_days=7";

            JsonNode response = restClient.get()
                    .uri(weatherUrl, lat, lon)
                    .retrieve()
                    .onStatus(s -> s != HttpStatus.OK, (req, res) -> {
                        log.error("天气API请求失败: status={}", res.getStatusCode());
                    })
                    .body(JsonNode.class);

            if (response == null) return null;

            ForecastData data = new ForecastData();
            data.current = parseCurrent(response.get("current"));

            JsonNode daily = response.get("daily");
            if (daily != null) {
                data.daily = new DailyData();
                data.daily.time = jsonArrayToList(daily.get("time"));
                data.daily.weatherCode = jsonIntList(daily.get("weather_code"));
                data.daily.tempMax = jsonDoubleList(daily.get("temperature_2m_max"));
                data.daily.tempMin = jsonDoubleList(daily.get("temperature_2m_min"));
                data.daily.precipProb = jsonIntList(daily.get("precipitation_probability_max"));
                data.daily.windSpeed = jsonDoubleList(daily.get("wind_speed_10m_max"));
                data.daily.uvIndex = jsonDoubleList(daily.get("uv_index_max"));
                data.daily.sunrise = jsonArrayToList(daily.get("sunrise"));
                data.daily.sunset = jsonArrayToList(daily.get("sunset"));
            }
            return data;

        } catch (Exception e) {
            log.error("天气数据获取异常", e);
            return null;
        }
    }

    private CurrentData parseCurrent(JsonNode node) {
        if (node == null) return null;
        try {
            CurrentData c = new CurrentData();
            c.temp = node.get("temperature_2m").asDouble();
            c.humidity = node.get("relative_humidity_2m").asInt();
            c.windSpeed = node.get("wind_speed_10m").asDouble();
            c.weatherCode = node.get("weather_code").asInt();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateDayIndex(String dateStr, List<String> forecastDates) {
        if (forecastDates == null || forecastDates.isEmpty()) return 0;
        try {
            for (int i = 0; i < forecastDates.size(); i++) {
                if (forecastDates.get(i).equals(dateStr)) return i;
            }
            // 查找最近的日期
            LocalDate target = LocalDate.parse(dateStr);
            int closest = 0;
            long minDiff = Long.MAX_VALUE;
            for (int i = 0; i < forecastDates.size(); i++) {
                LocalDate fd = LocalDate.parse(forecastDates.get(i));
                long diff = Math.abs(target.toEpochDay() - fd.toEpochDay());
                if (diff < minDiff) { minDiff = diff; closest = i; }
            }
            return closest;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getWeatherDesc(Integer code) {
        return WMO_CODES.getOrDefault(code, "未知");
    }

    private String formatTemp(Double temp) {
        return temp != null ? String.format("%.0f°C", temp) : "N/A";
    }

    private String formatUv(Double uv) {
        if (uv == null) return "N/A";
        if (uv <= 2) return "低(" + uv + ")";
        if (uv <= 5) return "中等(" + uv + ")";
        if (uv <= 7) return "高(" + uv + ")";
        if (uv <= 10) return "很高(" + uv + ")";
        return "极高(" + uv + ")";
    }

    private String generateTravelAdvice(String city, String weather, Double maxTemp) {
        if (weather.contains("雨")) return "建议携带雨具，注意路面湿滑";
        if (weather.contains("雪")) return "注意保暖防寒，路面可能结冰";
        if (maxTemp != null && maxTemp > 35) return "天气炎热，注意防暑降温，多饮水";
        if (maxTemp != null && maxTemp < 5) return "天气寒冷，注意防寒保暖";
        return "天气良好，适合出行游玩";
    }

    private String generateClothingAdvice(Double maxTemp, String weather) {
        if (maxTemp == null) return "请根据天气合理穿衣";
        if (maxTemp > 30) return "建议穿短袖、短裤、裙子等夏装，带防晒用品";
        if (maxTemp > 20) return "建议穿薄长袖、T恤，早晚可加薄外套";
        if (maxTemp > 10) return "建议穿长袖、薄外套或夹克";
        if (maxTemp > 0) return "建议穿厚外套、毛衣，注意保暖";
        return "建议穿羽绒服、棉衣，戴帽子围巾";
    }

    private WeatherResponse createErrorResponse(String city, String date, String errorMsg) {
        return new WeatherResponse(city, date, "获取失败", "获取失败",
                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A",
                "N/A", "N/A", errorMsg, errorMsg);
    }

    private List<String> jsonArrayToList(JsonNode arr) {
        if (arr == null || !arr.isArray()) return List.of();
        List<String> list = new ArrayList<>();
        for (JsonNode n : arr) list.add(n.asText());
        return list;
    }

    private List<Integer> jsonIntList(JsonNode arr) {
        if (arr == null) return List.of();
        List<Integer> list = new java.util.ArrayList<>();
        for (JsonNode n : arr) list.add(n.asInt());
        return list;
    }

    private List<Double> jsonDoubleList(JsonNode arr) {
        if (arr == null) return List.of();
        List<Double> list = new java.util.ArrayList<>();
        for (JsonNode n : arr) list.add(n.asDouble());
        return list;
    }

    // --- Open-Meteo 响应数据结构 ---

    static class ForecastData {
        CurrentData current;
        DailyData daily;
    }

    static class CurrentData {
        double temp;
        int humidity;
        double windSpeed;
        int weatherCode;
    }

    static class DailyData {
        List<String> time;
        List<Integer> weatherCode;
        List<Double> tempMax;
        List<Double> tempMin;
        List<Integer> precipProb;
        List<Double> windSpeed;
        List<Double> uvIndex;
        List<String> sunrise;
        List<String> sunset;
    }
}
