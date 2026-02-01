-- KEYS[1] = counter_key
-- ARGV[1] = window_size (seconds)
-- ARGV[2] = limit

local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

local count = redis.call("INCR", KEYS[1])

if count == 1 then
    redis.call("EXPIRE", KEYS[1], window)
end

if count > limit then
    return 0
else
    return 1
end
