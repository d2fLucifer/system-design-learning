package com.d2f.ratelimiter.algorithm.slidingcounter;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
public class SlidingWindowCounterAlgorithm implements RateLimiter {

    private final JedisPool jedisPool;

    public SlidingWindowCounterAlgorithm(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean allowRequest(String key, int capacity, int refillRatePerSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            long now = System.currentTimeMillis();
            long curWin = now / 1000;
            String curKey = "limit:swc:" + key + ":" + curWin;
            String preKey = "limit:swc:" + key + ":" + (curWin - 1);

            String curStr = jedis.get(curKey);
            long curVal = Long.parseLong(curStr != null ? curStr : "0");

            String preStr = jedis.get(preKey);
            long preVal = Long.parseLong(preStr != null ? preStr : "0");

            double weight = (1000.0 - (now % 1000)) / 1000.0;

            if (curVal + (preVal * weight) < capacity) {
                jedis.incr(curKey);
                jedis.expire(curKey, 2);
                return true;
            }
            return false;
        }
    }

    @Override
    public AlgorithmType getServiceType() {
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }
}
