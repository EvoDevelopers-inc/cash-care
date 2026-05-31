package evo.developers.com.cashcare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    public static final String AI_PROFILE_PREFIX = "cashcare:ai:profile:";
    public static final String AI_COOLDOWN_PREFIX = "cashcare:ai:cooldown:";

    private final StringRedisTemplate redisTemplate;

    public void save(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (DataAccessException e) {
            log.warn("Redis save failed for key {}: {}", key, e.getMessage());
        }
    }

    public void save(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException e) {
            log.warn("Redis save (TTL) failed for key {}: {}", key, e.getMessage());
        }
    }

    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            log.warn("Redis get failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            Boolean has = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(has);
        } catch (DataAccessException e) {
            log.warn("Redis exists failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    public Long getTtlSeconds(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (DataAccessException e) {
            log.warn("Redis TTL failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.warn("Redis delete failed for key {}: {}", key, e.getMessage());
        }
    }

    public static String aiProfileKey(String username) {
        return AI_PROFILE_PREFIX + username;
    }

    public static String aiCooldownKey(String username) {
        return AI_COOLDOWN_PREFIX + username;
    }
}
