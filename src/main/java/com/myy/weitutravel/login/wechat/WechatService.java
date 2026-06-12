package com.myy.weitutravel.login.wechat;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myy.weitutravel.common.config.WechatConfig;
import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
public class WechatService {

    private final WechatConfig config;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final Duration BIND_TOKEN_TTL = Duration.ofMinutes(10);
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final Set<String> RETRYABLE_ERRORS = Set.of("-1", "45011");

    public record WechatSession(String openid, String unionid, String sessionKey) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BindInfo {
        private String openid;
        private String unionid;
        private String sessionKey;
    }

    public WechatService(WechatConfig config, RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper) {
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
                .build();
    }

    // ── 小程序 code → session ─────────────────────────────────────

    public WechatSession code2session(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                config.getCode2sessionUrl(), config.getAppId(), config.getAppSecret(), code);
        return doGet(url, "code2session");
    }

    // ── 开放平台 code → access_token ───────────────────────────────

    public WechatSession code2accessToken(String code) {
        WechatConfig.Open open = config.getOpen();
        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                open.getAccessTokenUrl(), open.getAppId(), open.getAppSecret(), code);
        return doGet(url, "oauth2/access_token");
    }

    /**
     * 微信回调后，用 code 换 openid，返回 WechatSession。
     * 调用方根据 openid 判断已绑定还是需绑定。
     */
    public WechatSession exchangeCodeForOpenid(String code) {
        return code2accessToken(code);
    }

    /**
     * 取出 state 中存储的前端回调地址
     */
    public String consumeState(String state) {
        String key = "wechat:state:" + state;
        String redirectUrl = (String) redisTemplate.opsForValue().get(key);
        if (redirectUrl != null) {
            redisTemplate.delete(key);
        }
        return redirectUrl;
    }

    // ── 绑定凭证管理 ──────────────────────────────────────────────

    public String createBindToken(String openid, String unionid, String sessionKey) {
        String token = IdUtil.fastSimpleUUID();
        String key = Constants.WECHAT_BIND_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, new BindInfo(openid, unionid, sessionKey), BIND_TOKEN_TTL);
        return token;
    }

    public BindInfo consumeBindToken(String bindToken) {
        String key = Constants.WECHAT_BIND_TOKEN_PREFIX + bindToken;
        BindInfo info = (BindInfo) redisTemplate.opsForValue().get(key);
        if (info == null) {
            throw new BizException("绑定凭证已过期，请重新授权");
        }
        redisTemplate.delete(key);
        return info;
    }

    // ── 通用 HTTP GET + 重试 ──────────────────────────────────────

    private WechatSession doGet(String url, String apiName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(config.getReadTimeout()))
                .GET()
                .build();

        Exception lastException = null;
        for (int attempt = 0; attempt <= config.getMaxRetry(); attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(500L * attempt);
                }
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                int errcode = json.path("errcode").asInt(0);
                if (errcode == 0 || (json.has("openid") && !json.get("openid").asText().isEmpty())) {
                    return new WechatSession(
                            json.get("openid").asText(),
                            json.has("unionid") ? json.get("unionid").asText() : null,
                            json.has("session_key") ? json.get("session_key").asText()
                                    : json.has("access_token") ? json.get("access_token").asText() : null
                    );
                }

                String errmsg = json.path("errmsg").asText("未知错误");

                if (errcode == 40029 || errcode == 40163) {
                    throw new BizException("微信授权码无效或已过期，请重新授权");
                }
                if (errcode == 40125) {
                    log.error("微信 API({}) AppSecret 配置错误", apiName);
                    throw new BizException("微信登录服务配置异常");
                }
                if (errcode == 40226) {
                    throw new BizException("微信登录服务异常");
                }

                if (RETRYABLE_ERRORS.contains(String.valueOf(errcode))) {
                    log.warn("微信 API({}) 重试: attempt={}, errcode={}, errmsg={}", apiName, attempt + 1, errcode, errmsg);
                    lastException = new BizException("微信服务繁忙，请稍后重试");
                    continue;
                }

                log.error("微信 API({}) 失败: errcode={}, errmsg={}", apiName, errcode, errmsg);
                throw new BizException("微信登录失败");

            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == config.getMaxRetry()) {
                    lastException = e;
                } else {
                    log.warn("微信 API({}) 网络异常重试: attempt={}", apiName, attempt + 1, e);
                }
            }
        }

        log.error("微信 API({}) 重试耗尽", apiName, lastException);
        throw new BizException("微信服务繁忙，请稍后重试");
    }
}
