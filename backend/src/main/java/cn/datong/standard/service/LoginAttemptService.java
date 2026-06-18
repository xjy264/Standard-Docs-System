package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final String KEY_PREFIX = "login:fail:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.login-failure-limit:5}")
    private int failureLimit;
    @Value("${app.security.login-failure-window-seconds:600}")
    private long failureWindowSeconds;

    public void assertAllowed(String phone, HttpServletRequest request) {
        String key = key(phone, request);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Long.parseLong(value) >= failureLimit) {
                throw new BusinessException(429, "登录失败次数过多，请稍后再试");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Redis 异常时不阻断登录，避免缓存服务短暂异常导致全部用户无法登录。
        }
    }

    public void recordFailure(String phone, HttpServletRequest request) {
        String key = key(phone, request);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(failureWindowSeconds));
            }
        } catch (Exception ignored) {
        }
    }

    public void clear(String phone, HttpServletRequest request) {
        try {
            redisTemplate.delete(key(phone, request));
        } catch (Exception ignored) {
        }
    }

    private String key(String phone, HttpServletRequest request) {
        return KEY_PREFIX + (phone == null ? "" : phone) + ":" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
