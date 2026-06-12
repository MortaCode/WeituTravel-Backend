package com.myy.weitutravel.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    /** 小程序 */
    private String appId;
    private String appSecret;
    private String code2sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    /** 开放平台（网站/App） */
    private Open open = new Open();

    /** 通用 */
    private int connectTimeout = 5;
    private int readTimeout = 10;
    private int maxRetry = 2;

    @Data
    public static class Open {
        private String appId;
        private String appSecret;
        /** OAuth2 授权页 URL（公众号网页 / 开放平台扫码共用此接口换 token） */
        private String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect";
        private String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
        /** 微信回调后重定向到的后端地址，需在开放平台配置为授权回调域 */
        private String callbackUrl;
        /** 授权 scope：snsapi_base(静默) / snsapi_userinfo(弹窗) */
        private String scope = "snsapi_userinfo";
    }
}
