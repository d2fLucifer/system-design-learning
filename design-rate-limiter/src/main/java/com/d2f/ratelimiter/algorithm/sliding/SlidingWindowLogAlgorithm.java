package com.d2f.ratelimiter.algorithm.sliding;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
public class SlidingWindowLogAlgorithm implements RateLimiter {

    private final JedisPool jedisPool;

    public SlidingWindowLogAlgorithm(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean allowRequest(String key, int capacity, int refillRatePerSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            String redisKey = "limit:swl:" + key;
            long now = System.currentTimeMillis();

            jedis.zremrangeByScore(redisKey, 0, now - 1000);

            if (jedis.zcard(redisKey) < capacity) {
                jedis.zadd(redisKey, now, String.valueOf(now + Math.random()));
                jedis.expire(redisKey, 2);
                return true;
            }
            return false;
        }
    }

    @Override
    public AlgorithmType getServiceType() {
        return AlgorithmType.SLIDING_WINDOW_LOG;
    }
}
