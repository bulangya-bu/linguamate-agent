package com.example.javabigwork.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String KEY_PREFIX = "linguamate:cache:";
    private static final Duration FAILURE_BACKOFF = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private volatile long disabledUntilMillis;

    public RedisCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${linguamate.cache.redis.enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public String key(String namespace, Object... parts) {
        StringBuilder builder = new StringBuilder(KEY_PREFIX).append(namespace);
        for (Object part : parts) {
            builder.append(':').append(encodePart(part));
        }
        return builder.toString();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return read(key).map(json -> deserialize(json, type, key));
    }

    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        return read(key).map(json -> {
            try {
                JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
                return objectMapper.readValue(json, listType);
            } catch (Exception exception) {
                handleFailure("deserialize list", key, exception);
                return List.<T>of();
            }
        }).filter(list -> !list.isEmpty());
    }

    public boolean put(String key, Object value, Duration ttl) {
        if (!available() || value == null) {
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, ttl);
            }
            return true;
        } catch (Exception exception) {
            handleFailure("put", key, exception);
            return false;
        }
    }

    public void putIndexed(String indexKey, String key, Object value, Duration ttl) {
        if (put(key, value, ttl)) {
            index(indexKey, key, ttl);
        }
    }

    public void index(String indexKey, String key, Duration ttl) {
        if (!available()) {
            return;
        }
        try {
            redisTemplate.opsForSet().add(indexKey, key);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.expire(indexKey, ttl.plus(Duration.ofMinutes(5)));
            }
        } catch (Exception exception) {
            handleFailure("index", indexKey, exception);
        }
    }

    public void evict(String key) {
        if (!available()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            handleFailure("evict", key, exception);
        }
    }

    public void evictIndexed(String indexKey) {
        if (!available()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.opsForSet().members(indexKey);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            redisTemplate.delete(indexKey);
        } catch (Exception exception) {
            handleFailure("evict indexed", indexKey, exception);
        }
    }

    private Optional<String> read(String key) {
        if (!available()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception exception) {
            handleFailure("get", key, exception);
            return Optional.empty();
        }
    }

    private <T> T deserialize(String json, Class<T> type, String key) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            handleFailure("deserialize", key, exception);
            return null;
        }
    }

    private boolean available() {
        return enabled && System.currentTimeMillis() >= disabledUntilMillis;
    }

    private void handleFailure(String operation, String key, Exception exception) {
        disabledUntilMillis = System.currentTimeMillis() + FAILURE_BACKOFF.toMillis();
        log.debug("Redis cache {} failed for key={}, fallback to data source", operation, key, exception);
    }

    private String encodePart(Object part) {
        String raw = part == null ? "_" : part.toString().trim();
        if (raw.isEmpty()) {
            raw = "_";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
