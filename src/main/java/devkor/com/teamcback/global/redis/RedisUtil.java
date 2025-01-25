package devkor.com.teamcback.global.redis;

import devkor.com.teamcback.domain.search.entity.SearchLog;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j(topic = "Redis Util")
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 토큰의 경우 key가 refresh token, value가 user의 id
     */
    public void set(String key, String value, int minutes) {
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(value.getClass()));
        redisTemplate.opsForValue().set(key, value, minutes, TimeUnit.MINUTES);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }
}

