package com.myy.weitutravel.chat.mcp.tool;

import com.myy.weitutravel.chat.mcp.vo.TravelPlanVO.CostBreakdown;
import com.myy.weitutravel.chat.mcp.vo.TravelPlanVO.DayPlan;
import com.myy.weitutravel.chat.mcp.vo.TravelPlanVO.TravelPlanRequest;
import com.myy.weitutravel.chat.mcp.vo.TravelPlanVO.TravelPlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 旅游规划文档生成 MCP 工具
 * 根据用户出行信息自动生成结构化旅游行程
 */
@Slf4j
@Component("generateDocument")
public class DocumentTool {

    private static final Map<String, DestinationTemplate> TEMPLATES = Map.of(
            "北京", new DestinationTemplate(
                    new String[][]{
                            {"游览天安门广场，观看升旗仪式", "参观故宫博物院", "逛王府井小吃街，品尝北京小吃"},
                            {"登八达岭长城", "游览明十三陵", "后海酒吧街感受京城夜生活"},
                            {"游览颐和园", "参观圆明园遗址公园", "簋街品尝麻辣小龙虾"},
                            {"逛天坛公园", "漫步前门大街", "老舍茶馆看曲艺表演"},
                            {"游览798艺术区", "参观国家博物馆", "全聚德品尝北京烤鸭"}
                    }
            ),
            "成都", new DestinationTemplate(
                    new String[][]{
                            {"游览武侯祠", "逛锦里古街", "春熙路太古里购物晚餐"},
                            {"参观大熊猫繁育研究基地", "游览宽窄巷子", "品尝地道成都火锅"},
                            {"游览青城山", "参观都江堰", "九眼桥酒吧街放松"},
                            {"游览杜甫草堂", "逛人民公园喝盖碗茶", "欣赏川剧变脸"},
                            {"游览金沙遗址博物馆", "逛建设路小吃街", "IFS看熊猫雕塑打卡"}
                    }
            ),
            "三亚", new DestinationTemplate(
                    new String[][]{
                            {"抵达后海滩漫步", "椰梦长廊看日落", "第一市场品尝海鲜大餐"},
                            {"游览亚龙湾热带天堂森林公园", "亚龙湾海滩游泳", "百花谷商业街晚餐"},
                            {"蜈支洲岛一日游（潜水、摩托艇）", "海棠湾免税店购物"},
                            {"游览南山文化旅游区（南山寺、108米海上观音）", "天涯海角打卡"},
                            {"槟榔谷体验黎苗文化", "大东海海滩放松", "海鲜烧烤告别晚餐"}
                    }
            ),
            "昆明", new DestinationTemplate(
                    new String[][]{
                            {"游览翠湖公园", "参观云南大学", "南屏步行街品尝过桥米线"},
                            {"游览石林风景区", "参观九乡溶洞", "返回市区品尝野生菌火锅"},
                            {"游览滇池海埂公园喂海鸥", "参观云南民族村", "昆明老街散步"},
                            {"游览西山森林公园登龙门", "参观金马碧鸡坊", "东风广场购物"},
                            {"游览斗南花市（亚洲最大花卉市场）", "官渡古镇体验老昆明", "收拾行囊准备返程"}
                    }
            ),
            "西安", new DestinationTemplate(
                    new String[][]{
                            {"游览兵马俑博物馆", "参观华清宫", "观看《长恨歌》实景演出"},
                            {"游览西安古城墙骑自行车", "逛回民街品尝美食", "钟鼓楼夜景"},
                            {"参观陕西历史博物馆", "游览大雁塔", "大唐不夜城感受盛唐文化"},
                            {"游览华山（西峰索道上）", "长空栈道体验", "返回西安市区"},
                            {"参观小雁塔、西安博物院", "永兴坊非遗美食体验", "购买伴手礼"}
                    }
            ),
            "上海", new DestinationTemplate(
                    new String[][]{
                            {"游览外滩万国建筑群", "南京路步行街购物", "陆家嘴登东方明珠"},
                            {"参观上海博物馆", "逛田子坊文艺小街", "新天地夜生活"},
                            {"上海迪士尼乐园全日游"},
                            {"游览豫园城隍庙", "漫步武康路/安福路", "淮海路shopping"},
                            {"参观中华艺术宫（世博中国馆）", "静安寺、1933老场坊", "外滩夜景告别"}
                    }
            )
    );

    private static final Map<String, BigDecimal> CITY_FLIGHT_COSTS = Map.of(
            "三亚", new BigDecimal("1200"), "北京", new BigDecimal("800"),
            "成都", new BigDecimal("900"), "昆明", new BigDecimal("1000"),
            "西安", new BigDecimal("700"), "厦门", new BigDecimal("600"),
            "桂林", new BigDecimal("500"), "上海", new BigDecimal("800"),
            "杭州", new BigDecimal("600"), "广州", new BigDecimal("500")
    );

    @Tool(description = "根据目的地、天数、预算、偏好等信息，生成包含每日行程、费用明细、旅行贴士的结构化旅游规划文档")
    public TravelPlanResponse generateDocument(@ToolParam(description = "旅游规划参数") TravelPlanRequest request) {
        log.info("文档生成: 目的地={}, 天数={}, 预算={}, 偏好={}",
                request.destination(), request.days(), request.budget(), request.preference());

        DestinationTemplate template = TEMPLATES.getOrDefault(request.destination(),
                new DestinationTemplate(new String[][]{
                        {"抵达目的地，入住酒店休整", "探索城市中心区域", "品尝当地特色美食"},
                        {"游览城市最著名景点", "参观当地博物馆了解文化", "逛夜市或步行街"},
                        {"深度体验当地特色项目", "游览近郊风景区", "享用当地晚餐"}
                }));

        LocalDate startDate = LocalDate.now().plusDays(1);

        BigDecimal flightCost = CITY_FLIGHT_COSTS.getOrDefault(request.destination(), new BigDecimal("1000"));
        BigDecimal hotelCost = estimateHotelCost(request.days(), request.budget());
        BigDecimal attractionsCost = BigDecimal.valueOf(200L * request.days());
        BigDecimal mealsCost = BigDecimal.valueOf(150L * request.days());
        BigDecimal transportCost = BigDecimal.valueOf(80L * request.days());

        List<DayPlan> days = new ArrayList<>();
        for (int i = 0; i < request.days(); i++) {
            String[] activities = i < template.activities.length
                    ? template.activities[i]
                    : new String[]{"自由探索" + request.destination() + "特色区域", "体验当地文化活动", "享用" + request.destination() + "地道美食"};
            days.add(new DayPlan(
                    i + 1,
                    startDate.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    activities.length > 0 ? activities[0] : "自由探索",
                    activities.length > 1 ? activities[1] : "自由活动",
                    activities.length > 2 ? activities[2] : "品尝当地美食",
                    request.hotelInfo() != null ? "行程指定酒店" : "当地推荐酒店",
                    "含早/午/晚餐（按行程安排）",
                    List.of(
                            "景点门票: ¥" + attractionsCost.divide(BigDecimal.valueOf(request.days()), 0, RoundingMode.UP),
                            "餐饮: ¥" + mealsCost.divide(BigDecimal.valueOf(request.days()), 0, RoundingMode.UP),
                            "市内交通: ¥" + transportCost.divide(BigDecimal.valueOf(request.days()), 0, RoundingMode.UP)
                    )
            ));
        }

        BigDecimal totalCost = flightCost.add(hotelCost).add(attractionsCost).add(mealsCost).add(transportCost);

        CostBreakdown breakdown = new CostBreakdown(
                flightCost, hotelCost, attractionsCost, mealsCost, transportCost, totalCost.multiply(BigDecimal.valueOf(request.travelers() > 0 ? request.travelers() : 1))
        );

        String title = request.destination() + request.days() + "日"
                + (request.preference() != null ? request.preference() : "深度") + "游";

        String overview = request.days() + "天" + request.destination() + "之旅，"
                + (request.travelers() > 0 ? request.travelers() + "人同行，" : "")
                + "人均预算约" + totalCost + "元。涵盖" + request.destination()
                + "经典景点与特色体验，行程张弛有度，适合"
                + (request.preference() != null ? request.preference() + "类型" : "各类") + "游客。";

        return new TravelPlanResponse(
                title, request.destination(), request.days() + "天",
                request.budget(), overview, days, breakdown,
                List.of(
                        "建议提前预订热门景点门票，避免现场排队",
                        "出行前查看当地天气，合理搭配衣物",
                        "保管好个人财物，注意旅途安全",
                        "下载离线地图，方便在没有网络时使用",
                        "购买旅游意外险，出行更安心"
                ),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    private BigDecimal estimateHotelCost(int days, String budget) {
        if (budget != null && budget.contains("-")) {
            try {
                String[] parts = budget.split("-");
                int dailyBudget = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                return BigDecimal.valueOf((long) dailyBudget * days);
            } catch (Exception ignored) {}
        }
        return BigDecimal.valueOf(400L * days);
    }

    private record DestinationTemplate(String[][] activities) {}
}
