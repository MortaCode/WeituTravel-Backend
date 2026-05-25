package com.myy.weitutravel.chat.agent;

import com.myy.weitutravel.chat.agent.model.AgentPlan;
import com.myy.weitutravel.chat.agent.model.AgentTask;
import com.myy.weitutravel.chat.agent.model.IntentType;
import com.myy.weitutravel.chat.agent.model.TravelIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 任务规划与拆分服务
 * 将用户意图拆解为具体的 MCP 工具调用序列
 */
@Slf4j
@Service
public class TaskPlanningService {

    /**
     * 根据意图生成任务执行计划
     */
    public AgentPlan plan(TravelIntent intent) {
        log.info("任务规划: type={}, entities={}", intent.type().getName(), intent.entities());

        List<AgentTask> tasks = switch (intent.type()) {
            case TRAVEL_PLAN -> planTravel(intent);
            case WEATHER_QUERY -> planWeather(intent);
            case FLIGHT_QUERY -> planFlight(intent);
            case HOTEL_QUERY -> planHotel(intent);
            case ATTRACTION_QUERY -> planWeather(intent); // 景点查询先查天气
            case GENERAL_CHAT -> List.of();
        };

        String reasoning = buildReasoning(intent, tasks);

        return AgentPlan.builder()
                .intent(intent)
                .tasks(tasks)
                .reasoning(reasoning)
                .build();
    }

    /**
     * 旅行规划复合意图 → 拆分为多步子任务
     * 依赖关系：天气 → 机票 → 酒店 → 文档生成
     */
    private List<AgentTask> planTravel(TravelIntent intent) {
        String dest = intent.getEntity(TravelIntent.DESTINATION);
        String dep = intent.getEntity(TravelIntent.DEPARTURE);
        String date = intent.getEntity(TravelIntent.DATE);
        String days = intent.getEntity(TravelIntent.DAYS);
        String budget = intent.getEntity(TravelIntent.BUDGET);
        String preference = intent.getEntity(TravelIntent.PREFERENCE);
        String travelers = intent.getEntity(TravelIntent.TRAVELERS);

        List<AgentTask> tasks = new ArrayList<>();
        int order = 0;

        // Step 1: 查询目的地天气（优先，影响后续建议）
        if (dest != null) {
            tasks.add(AgentTask.builder()
                    .id("step-weather")
                    .description("查询" + dest + "天气，了解旅行期间气候状况")
                    .toolName("weatherQuery")
                    .toolPurpose("获取目的地天气，为穿衣建议和行程调整提供依据")
                    .order(order++)
                    .params(Map.of(TravelIntent.CITY, dest, TravelIntent.DATE, date != null ? date : "today"))
                    .required(true)
                    .build());
        }

        // Step 2: 查询机票（如果有出发地）
        if (dep != null && dest != null) {
            tasks.add(AgentTask.builder()
                    .id("step-flight")
                    .description("查询从" + dep + "到" + dest + "的机票")
                    .toolName("flightQuery")
                    .toolPurpose("获取航班信息、价格、时间，为出行提供交通选择")
                    .order(order++)
                    .dependencies(dest != null ? List.of("step-weather") : List.of())
                    .params(Map.of(
                            TravelIntent.DEPARTURE, dep,
                            TravelIntent.DESTINATION, dest,
                            TravelIntent.DATE, date != null ? date : "2026-05-26"
                    ))
                    .required(dep != null)
                    .build());
        }

        // Step 3: 查询酒店
        if (dest != null) {
            Map<String, String> hotelParams = new HashMap<>();
            hotelParams.put(TravelIntent.CITY, dest);
            if (date != null) hotelParams.put(TravelIntent.CHECK_IN, date);
            if (budget != null) {
                int budgetNum = extractBudgetNum(budget);
                if (budgetNum < 500) hotelParams.put("priceLevel", "budget");
                else if (budgetNum < 1200) hotelParams.put("priceLevel", "comfort");
                else hotelParams.put("priceLevel", "luxury");
            }
            hotelParams.put("minRating", "4");

            List<String> deps = new ArrayList<>();
            if (dest != null) deps.add("step-weather");
            if (dep != null) deps.add("step-flight");

            tasks.add(AgentTask.builder()
                    .id("step-hotel")
                    .description("查询" + dest + "酒店住宿信息")
                    .toolName("hotelQuery")
                    .toolPurpose("获取酒店列表、价格、设施，为用户提供住宿选择")
                    .order(order++)
                    .dependencies(deps)
                    .params(hotelParams)
                    .required(true)
                    .build());
        }

        // Step 4: 生成旅行规划文档（最后一步，依赖前面所有结果）
        if (dest != null) {
            Map<String, String> docParams = new HashMap<>();
            docParams.put(TravelIntent.DESTINATION, dest);
            docParams.put(TravelIntent.DAYS, days != null ? days : "3");
            if (budget != null) docParams.put(TravelIntent.BUDGET, budget);
            if (preference != null) docParams.put(TravelIntent.PREFERENCE, preference);
            if (travelers != null) docParams.put(TravelIntent.TRAVELERS, travelers);

            List<String> allDeps = tasks.stream().map(AgentTask::getId).toList();

            tasks.add(AgentTask.builder()
                    .id("step-document")
                    .description("生成" + dest + (days != null ? days : "") + "日游完整旅行规划文档")
                    .toolName("generateDocument")
                    .toolPurpose("将机票、酒店、天气等信息整合为结构化的旅行日程文档")
                    .order(order)
                    .dependencies(allDeps)
                    .params(docParams)
                    .required(true)
                    .build());
        }

        return tasks;
    }

    private List<AgentTask> planWeather(TravelIntent intent) {
        String city = intent.getEntity(TravelIntent.CITY);
        String date = intent.getEntity(TravelIntent.DATE);
        return List.of(AgentTask.builder()
                .id("step-weather")
                .description("查询" + (city != null ? city : "目标城市") + "天气")
                .toolName("weatherQuery")
                .toolPurpose("获取实时天气信息")
                .order(0)
                .params(Map.of(
                        TravelIntent.CITY, city != null ? city : "北京",
                        TravelIntent.DATE, date != null ? date : "today"
                ))
                .required(true)
                .build());
    }

    private List<AgentTask> planFlight(TravelIntent intent) {
        String departure = intent.getEntity(TravelIntent.DEPARTURE);
        String destination = intent.getEntity(TravelIntent.DESTINATION);
        String date = intent.getEntity(TravelIntent.DATE);
        return List.of(AgentTask.builder()
                .id("step-flight")
                .description("查询机票: " + departure + " → " + destination)
                .toolName("flightQuery")
                .toolPurpose("获取航班信息")
                .order(0)
                .params(Map.of(
                        TravelIntent.DEPARTURE, departure != null ? departure : "北京",
                        TravelIntent.DESTINATION, destination != null ? destination : "三亚",
                        TravelIntent.DATE, date != null ? date : "2026-05-26"
                ))
                .required(true)
                .build());
    }

    private List<AgentTask> planHotel(TravelIntent intent) {
        String city = intent.getEntity(TravelIntent.CITY);
        return List.of(AgentTask.builder()
                .id("step-hotel")
                .description("查询" + (city != null ? city : "目标城市") + "酒店")
                .toolName("hotelQuery")
                .toolPurpose("获取酒店列表")
                .order(0)
                .params(Map.of(TravelIntent.CITY, city != null ? city : "北京"))
                .required(true)
                .build());
    }

    private String buildReasoning(TravelIntent intent, List<AgentTask> tasks) {
        if (tasks.isEmpty()) return "无需调用工具，直接回答用户问题。";

        StringBuilder sb = new StringBuilder();
        sb.append("用户意图：").append(intent.type().getDescription()).append("。\n");
        sb.append("识别实体：").append(intent.entities().isEmpty() ? "无" : intent.entities().toString()).append("。\n");

        if (intent.type().isComposite()) {
            sb.append("该意图为复合意图，需要按依赖关系依次调用多个工具。\n");
            sb.append("执行顺序：天气（了解气候）→ 机票（交通安排）→ 酒店（住宿保障）→ 文档生成（整合输出）。\n");
        } else {
            sb.append("该意图为单一意图，直接调用对应工具即可。\n");
        }

        sb.append("共拆分为 ").append(tasks.size()).append(" 个子任务。");
        return sb.toString();
    }

    private int extractBudgetNum(String budget) {
        try {
            return Integer.parseInt(budget.replaceAll("[^0-9]", "").substring(0,
                    Math.min(4, budget.replaceAll("[^0-9]", "").length())));
        } catch (Exception e) {
            return 1000;
        }
    }
}
