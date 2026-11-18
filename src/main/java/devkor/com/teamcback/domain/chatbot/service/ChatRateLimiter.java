package devkor.com.teamcback.domain.chatbot.service;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_RATE_LIMITED;
import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatRateLimiter {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final StringRedisTemplate redisTemplate;
    private final ChatbotProperties properties;

    public void check(ChatCaller caller) {
        try {
            ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of(properties.rateLimit().dailyResetZone()));
            long burst = increment("chatbot:rate:minute:" + caller.key() + ":" + now.format(MINUTE),
                    Duration.between(now, now.plusMinutes(1).withSecond(0).withNano(0)));
            if (burst > properties.rateLimit().burstPerMinute()) {
                throw new GlobalException(CHATBOT_RATE_LIMITED);
            }
            long daily = increment("chatbot:rate:daily:" + caller.key() + ":" + now.format(DAY),
                    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.getZone())));
            int dailyLimit = caller.authenticated() ? properties.rateLimit().authenticatedDailyLimit()
                    : properties.rateLimit().anonymousDailyLimit();
            if (daily > dailyLimit) {
                throw new GlobalException(CHATBOT_RATE_LIMITED);
            }
        } catch (GlobalException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    private long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
        if (count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count;
    }
}
