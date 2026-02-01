package com.d2f.ratelimiter.algorithm.token;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.Map;

@Component
public class TokenBucketAlgorithm implements RateLimiter {

    private final JedisPool jedisPool;

    public TokenBucketAlgorithm(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean allowRequest(String key, int capacity, int refillRatePerSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            String redisKey = "limit:token:" + key;
            long now = System.currentTimeMillis();

            Map<String, String> data = jedis.hgetAll(redisKey);

            double tokens;
            long lastRefillTime;

            if (data == null || data.isEmpty()) {
                tokens = capacity;
                lastRefillTime = now;
            } else {
                tokens = Double.parseDouble(data.getOrDefault("tokens", String.valueOf(capacity)));
                lastRefillTime = Long.parseLong(data.getOrDefault("last_refill_time", String.valueOf(now)));
            }

            // Refill tokens
            long timePassed = now - lastRefillTime;
            double tokensToAdd = timePassed * (refillRatePerSecond / 1000.0);
            double newTokens = Math.min(capacity, tokens + tokensToAdd);

            if (newTokens >= 1) {
                newTokens -= 1;
                jedis.hset(redisKey, Map.of(
                        "tokens", String.valueOf(newTokens),
                        "last_refill_time", String.valueOf(now)));
                return true;
            }
            return false;
        }
    }

    @Override
    public AlgorithmType getServiceType() {
        return AlgorithmType.TOKEN_BUCKET;
    }
}
