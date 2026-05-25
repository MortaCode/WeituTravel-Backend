package com.myy.weitutravel.chat.mcp.tool;

import com.myy.weitutravel.chat.mcp.vo.WeatherVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@SpringBootTest
class WeatherToolTest {

    @Resource
    WeatherTool weatherTool;

    @Test
    void queryWeather() {
        WeatherVO.WeatherRequest request = new WeatherVO.WeatherRequest("上海", "2026-05-25");
        WeatherVO.WeatherResponse response = weatherTool.queryWeather(request);
        log.info("白天；{}， 晚上：{}", response.dayWeather(), response.nightWeather());
    }
}