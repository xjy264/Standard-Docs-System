package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AttachmentAccessTicketService {
    private static final String KEY_PREFIX = "attachment-ticket:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalTicket> fallbackTickets = new ConcurrentHashMap<>();

    @Value("${app.security.attachment-ticket-seconds:300}")
    private long ticketSeconds;

    public String issueItemAttachmentTicket(CurrentUser currentUser, Long attachmentId) {
        String token = randomToken();
        String value = "ITEM:" + attachmentId + ":" + currentUser.userId();
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, value, Duration.ofSeconds(ticketSeconds));
        } catch (Exception ignored) {
            fallbackTickets.put(token, new LocalTicket(value, Instant.now().plusSeconds(ticketSeconds)));
        }
        return token;
    }

    public void requireItemAttachmentTicket(Long attachmentId, String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(403, "文件访问票据无效或已过期");
        }
        String value = null;
        try {
            value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        } catch (Exception ignored) {
        }
        if (value == null) {
            LocalTicket localTicket = fallbackTickets.get(token);
            if (localTicket != null && localTicket.expiresAt().isAfter(Instant.now())) {
                value = localTicket.value();
            }
        }
        if (!Objects.equals(value == null ? null : value.split(":")[0], "ITEM")
                || !Objects.equals(value.split(":")[1], String.valueOf(attachmentId))) {
            throw new BusinessException(403, "文件访问票据无效或已过期");
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record LocalTicket(String value, Instant expiresAt) {
    }
}
