package com.myy.weitutravel.chat.agent;

import com.myy.weitutravel.chat.agent.model.IntentType;
import com.myy.weitutravel.chat.agent.model.TravelIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别服务
 * 通过正则匹配 + 实体提取，将用户自然语言输入分类为具体意图
 */
@Slf4j
@Service
public class IntentRecognitionService {

    // === 关键词匹配规则 ===

    private static final Map<IntentType, List<String>> INTENT_KEYWORDS = Map.of(
            IntentType.WEATHER_QUERY, List.of("天气", "气温", "温度", "下雨", "下雨吗", "带伞", "穿什么",
                    "冷不冷", "热不热", "气候", "降水量", "湿度", "风力"),
            IntentType.FLIGHT_QUERY, List.of("机票", "航班", "飞机", "飞", "票价", "航空公司",
                    "经济舱", "商务舱", "登机", "起飞"),
            IntentType.HOTEL_QUERY, List.of("酒店", "住宿", "旅馆", "民宿", "入住", "退房",
                    "房间", "套房", "标间", "大床房", "checkin", "checkout"),
            IntentType.ATTRACTION_QUERY, List.of("景点", "旅游景点", "有什么好玩", "推荐景点", "必去",
                    "打卡", "名胜", "攻略", "游记"),
            IntentType.TRAVEL_PLAN, List.of("规划", "行程", "安排", "计划", "攻略", "方案",
                    "日游", "几日游", "玩几天", "自由行", "定制", "设计路线",
                    "帮我", "组团", "报团", "跟团")
    );

    // === 实体提取正则 ===

    /** 城市名匹配（支持：北京、上海、三亚等） */
    private static final Pattern CITY_PATTERN = Pattern.compile(
            "(北京|上海|广州|深圳|成都|重庆|杭州|南京|武汉|西安|郑州|苏州|天津|长沙|东莞|" +
                    "沈阳|青岛|合肥|佛山|宁波|昆明|大连|福州|厦门|哈尔滨|济南|温州|南宁|长春|" +
                    "泉州|贵阳|南昌|太原|金华|徐州|嘉兴|惠州|烟台|中山|珠海|常州|南通|兰州|" +
                    "海口|三亚|桂林|丽江|大理|张家界|拉萨|乌鲁木齐|呼和浩特|银川|西宁)");

    /** 日期提取 */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日号]?|\\d{1,2}月\\d{1,2}[日号]|" +
                    "明天|后天|大后天|今天|下?周[一二三四五六日天]|下周)");

    /** 天数提取 */
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*[日天]");

    /** 预算提取 */
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(\\d+[,，]?\\d*)\\s*[-~至到]\\s*(\\d+[,，]?\\d*)\\s*[元块]?\\s*(预算|左右|人均|每人)?");

    /** 人数提取 */
    private static final Pattern TRAVELERS_PATTERN = Pattern.compile(
            "(\\d+)\\s*[个位人]|([一两三四五六七八九十]+)\\s*[个位人]");

    /** 偏好提取 */
    private static final Pattern PREFERENCE_PATTERN = Pattern.compile(
            "(文化|历史|美食|自然|风光|亲子|蜜月|蜜月游|情侣|摄影|购物|休闲|度假|" +
                    "探险|徒步|穷游|豪华|经济|文艺|小众)");

    private static final Map<String, Integer> CN_NUM = Map.of(
            "一", 1, "两", 2, "三", 3, "四", 4, "五", 5, "六", 6, "七", 7, "八", 8, "九", 9, "十", 10
    );

    /**
     * 识别用户意图
     */
    public TravelIntent recognize(String userInput) {
        log.info("意图识别开始: {}", userInput.length() > 50 ? userInput.substring(0, 50) + "..." : userInput);

        // 1. 计算各意图匹配得分
        Map<IntentType, Integer> scores = new EnumMap<>(IntentType.class);
        for (IntentType type : IntentType.values()) {
            int score = calculateKeywordScore(userInput, INTENT_KEYWORDS.getOrDefault(type, List.of()));
            if (score > 0) scores.put(type, score);
        }

        // 2. 检查是否为复合意图（旅行规划类关键词 + 多个单一意图关键词）
        boolean hasPlanKeywords = calculateKeywordScore(userInput,
                INTENT_KEYWORDS.get(IntentType.TRAVEL_PLAN)) > 0;
        List<Map.Entry<IntentType, Integer>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<IntentType, Integer>comparingByValue().reversed())
                .toList();

        IntentType primaryIntent;
        double confidence;
        String reasoning;

        if (sorted.isEmpty()) {
            primaryIntent = IntentType.GENERAL_CHAT;
            confidence = 0.3;
            reasoning = "未匹配到明显的旅行意图关键词";
        } else if (hasPlanKeywords && sorted.size() >= 2) {
            // 有旅行规划关键词且有其他意图 → 复合意图
            primaryIntent = IntentType.TRAVEL_PLAN;
            confidence = 0.85;
            reasoning = "检测到旅行规划意图，同时包含" + sorted.size() + "个相关子意图";
        } else {
            primaryIntent = sorted.get(0).getKey();
            int hits = sorted.get(0).getValue();
            confidence = Math.min(0.95, 0.4 + hits * 0.15);
            reasoning = "匹配到" + primaryIntent.getName() + "关键词，命中" + hits + "个";
        }

        // 3. 提取实体
        TravelIntent intent = TravelIntent.of(primaryIntent, confidence, userInput, reasoning);
        intent = extractEntities(intent, userInput);

        log.info("意图识别结果: type={}, confidence={}, entities={}", intent.type().getName(), intent.confidence(), intent.entities());
        return intent;
    }

    /**
     * 计算关键词命中得分
     */
    private int calculateKeywordScore(String input, List<String> keywords) {
        int score = 0;
        String lower = input.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw)) score++;
        }
        return score;
    }

    /**
     * 从输入中提取实体
     */
    private TravelIntent extractEntities(TravelIntent intent, String input) {
        TravelIntent result = intent;

        // 提取城市
        List<String> cities = extractCities(input);
        if (!cities.isEmpty()) {
            result = result.withEntity(TravelIntent.CITY, cities.get(0));
            if (cities.size() >= 2) {
                result = result.withEntity(TravelIntent.DEPARTURE, cities.get(0));
                result = result.withEntity(TravelIntent.DESTINATION, cities.get(1));
            } else {
                result = result.withEntity(TravelIntent.DESTINATION, cities.get(0));
            }
        }

        // 提取日期
        Matcher dateMatcher = DATE_PATTERN.matcher(input);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1);
            result = result.withEntity(TravelIntent.DATE, normalizeDate(dateStr));
        }

        // 提取天数
        Matcher daysMatcher = DAYS_PATTERN.matcher(input);
        if (daysMatcher.find()) {
            result = result.withEntity(TravelIntent.DAYS, daysMatcher.group(1));
        }

        // 提取预算
        Matcher budgetMatcher = BUDGET_PATTERN.matcher(input);
        if (budgetMatcher.find()) {
            String min = budgetMatcher.group(1).replaceAll("[,，]", "");
            String max = budgetMatcher.group(2).replaceAll("[,，]", "");
            result = result.withEntity(TravelIntent.BUDGET, min + "-" + max);
        }

        // 提取人数
        Matcher travelersMatcher = TRAVELERS_PATTERN.matcher(input);
        if (travelersMatcher.find()) {
            String numStr = travelersMatcher.group(1) != null ? travelersMatcher.group(1)
                    : String.valueOf(CN_NUM.getOrDefault(travelersMatcher.group(2), 2));
            result = result.withEntity(TravelIntent.TRAVELERS, numStr);
        }

        // 提取偏好
        Matcher prefMatcher = PREFERENCE_PATTERN.matcher(input);
        if (prefMatcher.find()) {
            result = result.withEntity(TravelIntent.PREFERENCE, prefMatcher.group(1) + "游");
        }

        return result;
    }

    private List<String> extractCities(String input) {
        List<String> cities = new ArrayList<>();
        Matcher m = CITY_PATTERN.matcher(input);
        while (m.find()) {
            cities.add(m.group(1));
        }
        return cities;
    }

    private String normalizeDate(String raw) {
        LocalDate today = LocalDate.now();
        return switch (raw) {
            case "今天" -> today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "明天" -> today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "后天" -> today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "大后天" -> today.plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);
            default -> {
                // 尝试解析 "5月25日" 格式
                Matcher m = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]?").matcher(raw);
                if (m.find()) {
                    int month = Integer.parseInt(m.group(1));
                    int day = Integer.parseInt(m.group(2));
                    LocalDate date = LocalDate.of(today.getYear(), month, day);
                    if (date.isBefore(today)) date = date.plusYears(1);
                    yield date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                // 尝试解析 "下周一" 格式
                Matcher wm = Pattern.compile("下周([一二三四五六日天])").matcher(raw);
                if (wm.find()) {
                    String dayCn = wm.group(1);
                    int dayOfWeek = "一二三四五六日天".indexOf(dayCn) + 1;
                    LocalDate nextMonday = today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                    yield nextMonday.plusDays(dayOfWeek - 1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                yield raw.replaceAll("[年月]", "-").replaceAll("[日号]", "");
            }
        };
    }
}
