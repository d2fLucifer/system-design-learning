-- KEYS[1] = log_key (ZSET)
-- ARGV[1] = window_size (ms)
-- ARGV[2] = limit
-- ARGV[3] = now (epoch ms)

local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local start_time = now - window

redis.call("ZREMRANGEBYSCORE", KEYS[1], 0, start_time)

local count = redis.call("ZCARD", KEYS[1])

if count >= limit then
    return 0
else
    redis.call("ZADD", KEYS[1], now, now)
    redis.call("PEXPIRE", KEYS[1], window)
    return 1
end
