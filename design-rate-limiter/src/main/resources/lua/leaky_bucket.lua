-- KEYS[1] = queue_size_key
-- KEYS[2] = last_leak_ts_key
-- ARGV[1] = capacity
-- ARGV[2] = leak_rate (req/sec)
-- ARGV[3] = now (epoch seconds)

local capacity = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local queue = tonumber(redis.call("GET", KEYS[1])) or 0
local last_ts = tonumber(redis.call("GET", KEYS[2])) or now

-- leak
local delta = math.max(0, now - last_ts)
local leaked = delta * leak_rate
queue = math.max(0, queue - leaked)

if queue >= capacity then
    redis.call("SET", KEYS[1], queue)
    redis.call("SET", KEYS[2], now)
    return 0
else
    queue = queue + 1
    redis.call("SET", KEYS[1], queue)
    redis.call("SET", KEYS[2], now)
    return 1
end
