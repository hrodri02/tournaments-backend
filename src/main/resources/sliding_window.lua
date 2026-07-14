-- Atomic sliding-window-counter rate limit check.
--
-- KEYS[1] : current window counter key  (ratelimit:{ip}:{windowNumber})
-- KEYS[2] : previous window counter key (ratelimit:{ip}:{windowNumber-1})
-- ARGV[1] : limit  - max requests allowed per window
-- ARGV[2] : weight - fraction of the previous window still inside the sliding
--                    window, i.e. 1 - (elapsed / windowSize), in [0, 1]
-- ARGV[3] : ttl    - seconds to keep the current window key (2 * windowSeconds)
--
-- Returns { allowed, estimate } where allowed is 1/0 and estimate is the
-- weighted request count (floored). The INCR happens before the decision, so a
-- rejected request still consumes a slot — recovery is within one window.

local limit  = tonumber(ARGV[1])
local weight = tonumber(ARGV[2])
local ttl    = tonumber(ARGV[3])

-- Increment the current window; set TTL only on the first hit so the key
-- self-expires and we avoid resetting the expiry on every request.
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ttl)
end

local previous = tonumber(redis.call('GET', KEYS[2]) or '0')

local estimate = current + (previous * weight)

local allowed = 0
if estimate <= limit then
    allowed = 1
end

return { allowed, math.floor(estimate) }
