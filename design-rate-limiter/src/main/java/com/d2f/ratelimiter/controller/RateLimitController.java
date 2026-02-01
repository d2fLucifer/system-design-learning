package com.d2f.ratelimiter.controller;

import com.d2f.ratelimiter.config.RateLimiterFactory;
import com.d2f.ratelimiter.limiter.AlgorithmType;
import com.d2f.ratelimiter.limiter.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RateLimitController {

    private final RateLimiterFactory rateLimiterFactory;

    public RateLimitController(RateLimiterFactory rateLimiterFactory) {
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @GetMapping("/test-limit")
    public ResponseEntity<String> testLimit(
            @RequestParam(name = "algo", defaultValue = "TOKEN_BUCKET") AlgorithmType algo,
            @RequestParam(name = "key", defaultValue = "user_default") String key,
            @RequestParam(name = "capacity", defaultValue = "10") int capacity,
            @RequestParam(name = "refillRate", defaultValue = "1") int refillRate) {
        try {
            RateLimiter limiter = rateLimiterFactory.getLimiter(algo);
            if (limiter == null)
                return ResponseEntity.badRequest().body("Unknown algo: " + algo);

            boolean allowed = limiter.allowRequest(key, capacity, refillRate);
            return allowed ? ResponseEntity.ok("[" + algo + "] Allowed: " + key)
                    : ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("[" + algo + "] Blocked: " + key);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-limit-bucket")
    public ResponseEntity<String> testLimitBucket(
            @RequestParam(name = "key", defaultValue = "default_user") String key,
            @RequestParam(name = "capacity", defaultValue = "10") int capacity,
            @RequestParam(name = "refillRate", defaultValue = "1") int refillRate) {
        return testLimit(AlgorithmType.TOKEN_BUCKET, key, capacity, refillRate);
    }
}
