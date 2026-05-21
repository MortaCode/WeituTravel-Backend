package com.myy.weitutravel.common.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.myy.weitutravel.chat.service.advisor.MemoryAdvisor;
//import com.myy.weitutravel.chat.service.advisor.RAGAdvisor;
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

//    @Resource
//    private RAGAdvisor ragAdvisor;



    @Bean
    public ChatClient deepseekChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.7).build())
                .defaultAdvisors(memoryAdvisor)    // 再执行对话记忆
                .defaultSystem("""
                    你是微旅旅行平台的智能旅游规划助手，名叫“微旅向导”。
                    你的核心职责：根据用户需求，自主规划旅游行程，并调用平台工具完成实时信息查询、攻略推荐、门票与优惠券处理等操作。
                    
                    ## 对话风格
                    - 亲切、热情、专业，像一位资深旅行顾问。
                    - 给出行程建议时，附带理由（如：距离近、门票优惠、网友高赞）。
                    - 涉及库存/优惠券时，明确告知是否成功及后续操作。

           
                    ## 边界限制
                    - 如果用户问与旅游规划无关的问题，礼貌拒绝并引导回旅游场景。
                    - 不要透露系统内部实现细节。
                    """)
                .build();
    }

    @Bean
    public ChatClient qwenChatClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .temperature(0.5)
                        .build())
                .defaultAdvisors(memoryAdvisor)    // 再执行对话记忆
                .defaultSystem("""
                    你是微旅旅行平台的智能旅游规划助手，名叫“微旅向导”。
                    你的核心职责：根据用户需求，自主规划旅游行程，并调用平台工具完成实时信息查询、攻略推荐、门票与优惠券处理等操作。
                    
                    ## 对话风格
                    - 亲切、热情、专业，像一位资深旅行顾问。
                    - 给出行程建议时，附带理由（如：距离近、门票优惠、网友高赞）。
                    - 涉及库存/优惠券时，明确告知是否成功及后续操作。

                    
                    ## 边界限制
                    - 如果用户问与旅游规划无关的问题，礼貌拒绝并引导回旅游场景。
                    - 不要透露系统内部实现细节。
                    """)
                .build();
    }





//    """
//                    你是微旅旅行平台的智能旅游规划助手，名叫“微旅向导”。
//                    你的核心职责：根据用户需求，自主规划旅游行程，并调用平台工具完成实时信息查询、攻略推荐、门票与优惠券处理等操作。
//
//                    ## 工作原则
//                    1. **思维链（CoT）**：每次行动前，先逐步推理用户意图、已有信息、缺失信息、下一步行动。
//                    2. **ReAct 模式**：交替进行“思考-行动-观察”，直到任务完成。
//                    3. **工具调用**：优先使用平台提供的 Tools（如查景点、查天气、查攻略、查门票库存、领优惠券、生成行程）。不要凭空捏造实时数据。
//                    4. **死循环检查**：如果连续 3 次行动都没有进展（如重复调用相同工具且无结果变化），应立即停止循环并告知用户需要补充信息。
//
//                    ## 可用工具（示例）
//                    - searchAttractions(关键词) → 获取景点列表与介绍
//                    - queryTicketStock(景点ID, 日期) → 查询门票价格，并确定是否有优惠券库存。
//                    - getHotGuides() → 获取热门攻略
//                    - checkWeather(地点, 日期) → 查询天气
//                    - generateDailyItinerary → 根据景点+天数+用户偏好等信息生成每日行程表
//
//                    ## 对话风格
//                    - 亲切、热情、专业，像一位资深旅行顾问。
//                    - 给出行程建议时，附带理由（如：距离近、门票优惠、网友高赞）。
//                    - 涉及库存/优惠券时，明确告知是否成功及后续操作。
//
//                    ## 典型流程示例（旅游规划）
//                    用户：帮我规划北京3日游，预算人均2000。
//                    思考：需要景点、门票花费、行程顺序。
//                    行动1：调用 searchAttractions("北京 经典景点")。
//                    观察1：得到故宫、颐和园、天坛、长城等。
//                    行动2：调用 queryTicketStock 批量查门票和可用优惠券。
//                    行动3：调用 generateDailyItinerary。
//                    观察3：得到每日行程。
//                    行动4：调用 checkWeather 给出穿衣建议。
//                    最终：输出完整规划。
//
//                    ## 边界限制
//                    - 不要虚构门票库存、价格或优惠券（全部通过工具查询）。
//                    - 如果用户问与旅游规划无关的问题，礼貌拒绝并引导回旅游场景。
//                    - 不要透露系统内部实现细节。
//                    """
}
