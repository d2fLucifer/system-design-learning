package com.d2f.ratelimiter.config;

import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RateLimiterFactory {

    private final Map<AlgorithmType, RateLimiter> limiters = new HashMap<>();

    public RateLimiterFactory(List<RateLimiter> strategies) {
        // Automatically register all RateLimiter strategies found in context
        strategies.forEach(strategy -> limiters.put(strategy.getServiceType(), strategy));
    }

    public RateLimiter getLimiter(AlgorithmType type) {
        return limiters.get(type);
    }
}
