package com.myy.weitutravel.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsConfig {

    private String provider = "aliyun";

    private Aliyun aliyun = new Aliyun();

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
        private String templateCode;
        private String endpoint = "dysmsapi.aliyuncs.com";
    }

    @Data
    public static class RateLimit {
        private int perMinute = 1;
        private int perHour = 5;
        private int perDay = 10;
    }
}
