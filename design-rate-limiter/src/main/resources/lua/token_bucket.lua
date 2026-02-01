-- KEYS[1] = rate limit key (e.g. rate_limit:user123)
-- ARGV[1] = capacity
-- ARGV[2] = refill_rate (tokens per second)
-- ARGV[3] = current_timestamp (milliseconds)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Redis hash fields
local tokens_key = "tokens"
local ts_key = "last_refill_ts"

-- Get current state
local data = redis.call("HMGET", key, tokens_key, ts_key)
local tokens = tonumber(data[1])
local last_refill_ts = tonumber(data[2])

-- First request: initialize bucket
if tokens == nil then
    tokens = capacity
    last_refill_ts = now
end

-- Calculate how many tokens to refill
local delta_ms = now - last_refill_ts
local refill_tokens = (delta_ms / 1000) * refill_rate

-- Refill and clamp to capacity
tokens = math.min(capacity, tokens + refill_tokens)
last_refill_ts = now

local allowed = 0

-- Consume token if possible
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

-- Persist state
redis.call("HMSET", key,
    tokens_key, tokens,
    ts_key, last_refill_ts
)

-- Optional TTL to avoid stale keys
redis.call("EXPIRE", key, 3600)

-- Return result
return allowed
