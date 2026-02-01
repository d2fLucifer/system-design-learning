package com.d2f.ratelimiter.algorithm.leaky;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.Map;

@Component
public class LeakyBucketAlgorithm implements RateLimiter {

    private final JedisPool jedisPool;

    public LeakyBucketAlgorithm(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean allowRequest(String key, int capacity, int refillRatePerSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            String redisKey = "limit:leaky:" + key;
            long now = System.currentTimeMillis();
            Map<String, String> data = jedis.hgetAll(redisKey);

            double water = Double.parseDouble(data.getOrDefault("water", "0"));
            long lastLeak = Long.parseLong(data.getOrDefault("last_leak", String.valueOf(now)));

            double leaked = (now - lastLeak) * (refillRatePerSecond / 1000.0);
            water = Math.max(0, water - leaked);

            if (water + 1 <= capacity) {
                jedis.hset(redisKey, Map.of(
                        "water", String.valueOf(water + 1),
                        "last_leak", String.valueOf(now)));
                return true;
            }
            return false;
        }
    }

    @Override
    public AlgorithmType getServiceType() {
        return AlgorithmType.LEAKY_BUCKET;
    }
}
