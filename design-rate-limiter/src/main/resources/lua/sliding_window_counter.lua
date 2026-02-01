-- KEYS[1] = current_window_key
-- KEYS[2] = previous_window_key
-- ARGV[1] = window_size (seconds)
-- ARGV[2] = limit
-- ARGV[3] = now (epoch seconds)

local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local current_window = math.floor(now / window)
local window_start = current_window * window
local prev_window_start = window_start - window

local current_count = tonumber(redis.call("GET", KEYS[1])) or 0
local prev_count = tonumber(redis.call("GET", KEYS[2])) or 0

local elapsed = now - window_start
local weight = (window - elapsed) / window

local estimated = current_count + prev_count * weight

if estimated >= limit then
    return 0
else
    local new_count = redis.call("INCR", KEYS[1])
    if new_count == 1 then
        redis.call("EXPIRE", KEYS[1], window * 2)
    end
    return 1
end
