package com.myy.weitutravel.login.sms;

import com.myy.weitutravel.common.config.SmsConfig;
import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsProvider smsProvider;
    private final SmsConfig smsConfig;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final String RATE_LIMIT_KEY = "sms:rl:%s:%s";

    public void sendCode(String mobile) {
        checkRateLimit(mobile);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = Constants.SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(key, code, CODE_TTL);

        smsProvider.send(mobile, "{\"code\":\"" + code + "\"}");
    }

    private void checkRateLimit(String mobile) {
        SmsConfig.RateLimit rl = smsConfig.getRateLimit();
        checkWindow(mobile, "minute", rl.getPerMinute(), Duration.ofMinutes(1));
        checkWindow(mobile, "hour", rl.getPerHour(), Duration.ofHours(1));
        checkWindow(mobile, "day", rl.getPerDay(), Duration.ofDays(1));
    }

    private void checkWindow(String mobile, String window, int limit, Duration ttl) {
        String key = String.format(RATE_LIMIT_KEY, window, mobile);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1) {
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (count > limit) {
            log.warn("短信发送频率超限: mobile={}, window={}, count={}", mobile, window, count);
            throw new BizException("发送过于频繁，请稍后再试");
        }
    }

    public boolean verifyCode(String mobile, String code) {
        String key = Constants.SMS_CODE_PREFIX + mobile;
        String cached = (String) redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return false;
        }
        if (cached.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
