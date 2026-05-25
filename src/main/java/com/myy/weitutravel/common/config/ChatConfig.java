package com.myy.weitutravel.common.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.myy.weitutravel.chat.mcp.tool.DocumentTool;
import com.myy.weitutravel.chat.mcp.tool.FlightTool;
import com.myy.weitutravel.chat.mcp.tool.HotelTool;
import com.myy.weitutravel.chat.mcp.tool.WeatherTool;
import com.myy.weitutravel.chat.service.advisor.MemoryAdvisor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Resource
    private MemoryAdvisor memoryAdvisor;

    @Resource
    private FlightTool flightTool;

    @Resource
    private WeatherTool weatherTool;

    @Resource
    private HotelTool hotelTool;

    @Resource
    private DocumentTool documentTool;

    private static final String SYSTEM_PROMPT = """
            你是微旅旅行平台的智能旅游规划助手，名叫"微旅向导"。
            你的核心职责：根据用户需求，自主规划旅游行程，并调用平台工具完成实时信息查询、攻略推荐、门票与优惠券处理等操作。

            ## 可用工具
            - flightQuery: 查询机票信息，参数{departure(出发城市), arrival(目的城市), date(日期yyyy-MM-dd)}
            - weatherQuery: 查询天气信息，参数{city(城市), date(日期yyyy-MM-dd)}
            - hotelQuery: 查询酒店信息，参数{city(城市), checkIn(入住日期), checkOut(离店日期), priceLevel(价格档次:budget/comfort/luxury), minRating(最低评分1-5)}
            - generateDocument: 生成旅游规划文档，参数{destination(目的地), days(天数), budget(预算), preference(偏好), travelers(人数), flightInfo(机票JSON), hotelInfo(酒店JSON)}

            ## 工作原则 - 严格遵守
            1. 涉及机票/天气/酒店等实时信息，必须调用工具获取，严禁虚构数据（如价格、航班号、天气等）
            2. 多工具组合：机票查询 → 天气查询 → 酒店查询 → 文档生成，一站式规划
            3. 行程完成后调用 generateDocument 生成正式规划文档
            4. 不要重复调用同一个工具，前一步已有结果则直接使用

            ## 对话风格
            - 亲切、热情、专业，像一位资深旅行顾问
            - 给出行程建议时附带理由（如：距离近、门票优惠、网友高赞）

            ## 边界限制
            - 只回答与旅游规划相关的问题，其他问题礼貌拒绝并引导回旅游场景
            """;

    @Bean
    public ChatClient deepseekChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.7).build())
                .defaultTools(flightTool, weatherTool, hotelTool, documentTool)
                .defaultAdvisors(memoryAdvisor)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Bean
    public ChatClient qwenChatClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .temperature(0.5)
                        .build())
                .defaultTools(flightTool, weatherTool, hotelTool, documentTool)
                .defaultAdvisors(memoryAdvisor)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

}
