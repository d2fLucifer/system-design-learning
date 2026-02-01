package com.d2f.ratelimiter.algorithm.fixed;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
public class FixedWindowAlgorithm implements RateLimiter {

    private final JedisPool jedisPool;

    public FixedWindowAlgorithm(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean allowRequest(String key, int capacity, int refillRatePerSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            String redisKey = "limit:fixed:" + key;
            long count = jedis.incr(redisKey);
            if (count == 1) {
                jedis.expire(redisKey, 1);
            }
            return count <= capacity;
        }
    }

    @Override
    public AlgorithmType getServiceType() {
        return AlgorithmType.FIXED_WINDOW;
    }
}
