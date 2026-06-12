package com.myy.weitutravel.login.sms;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myy.weitutravel.common.config.SmsConfig;
import com.myy.weitutravel.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun", matchIfMissing = true)
@RequiredArgsConstructor
public class AliyunSmsProvider implements SmsProvider {

    private final SmsConfig smsConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String VERSION = "2017-05-25";
    private static final String ACTION = "SendSms";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE_VERSION = "1.0";

    @Override
    public boolean isConfigured() {
        SmsConfig.Aliyun cfg = smsConfig.getAliyun();
        return cfg.getAccessKeyId() != null && !cfg.getAccessKeyId().isBlank()
                && cfg.getAccessKeySecret() != null && !cfg.getAccessKeySecret().isBlank()
                && cfg.getSignName() != null && !cfg.getSignName().isBlank()
                && cfg.getTemplateCode() != null && !cfg.getTemplateCode().isBlank();
    }

    @Override
    public void send(String mobile, String templateParam) {
        if (!isConfigured()) {
            log.warn("阿里云短信未配置，无法发送短信到 {}", mobile);
            throw new BizException("短信服务未配置");
        }

        SmsConfig.Aliyun cfg = smsConfig.getAliyun();

        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", cfg.getAccessKeyId());
        params.put("Action", ACTION);
        params.put("Format", "JSON");
        params.put("PhoneNumbers", mobile);
        params.put("RegionId", "cn-hangzhou");
        params.put("SignName", cfg.getSignName());
        params.put("SignatureMethod", SIGNATURE_METHOD);
        params.put("SignatureNonce", IdUtil.fastSimpleUUID());
        params.put("SignatureVersion", SIGNATURE_VERSION);
        params.put("TemplateCode", cfg.getTemplateCode());
        params.put("TemplateParam", templateParam);
        params.put("Timestamp", DateUtil.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss'Z'"));
        params.put("Version", VERSION);
        params.put("OutId", IdUtil.fastSimpleUUID());

        String signature = sign(params, cfg.getAccessKeySecret());
        params.put("Signature", signature);

        String queryString = buildQuery(params);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + cfg.getEndpoint() + "/?" + queryString))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            String code = json.path("Code").asText();
            if (!"OK".equals(code)) {
                log.error("阿里云短信发送失败: code={}, message={}, mobile={}", code, json.path("Message").asText(), mobile);
                throw new BizException("短信发送失败，请稍后重试");
            }

            log.info("短信发送成功: mobile={}", mobile);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常: mobile={}", mobile, e);
            throw new BizException("短信服务异常，请稍后重试");
        }
    }

    private String sign(TreeMap<String, String> params, String accessKeySecret) {
        String canonical = buildQuery(params);
        String stringToSign = "GET" + "&" + percentEncode("/") + "&" + percentEncode(canonical);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(percentEncode(e.getKey())).append("=").append(percentEncode(e.getValue()));
        }
        return sb.toString();
    }

    private String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
