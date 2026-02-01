package com.d2f.ratelimiter.limiter;

public interface RateLimiter {
    boolean allowRequest(String key, int capacity, int refillRatePerSecond);

    AlgorithmType getServiceType();
}
