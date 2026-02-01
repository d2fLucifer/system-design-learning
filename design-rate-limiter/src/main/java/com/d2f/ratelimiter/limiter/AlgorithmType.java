package com.d2f.ratelimiter.limiter;

public enum AlgorithmType {
    FIXED_WINDOW,
    TOKEN_BUCKET,
    LEAKY_BUCKET,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER
}
