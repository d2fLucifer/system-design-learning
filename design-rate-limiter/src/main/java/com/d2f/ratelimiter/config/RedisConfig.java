package com.d2f.ratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Disable JMX to avoid "InstanceAlreadyExistsException" during restarts
        poolConfig.setJmxEnabled(false);

        // Use 127.0.0.1 for consistent IPv4 connection on macOS
        return new JedisPool(poolConfig, "127.0.0.1", 6379);
    }
}
