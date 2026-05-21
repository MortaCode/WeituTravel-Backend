local tempThumbKey = KEYS[1]      -- 临时计数键（如 thumb:temp:{timeSlice}）
local userThumbKey = KEYS[2]      -- 用户点赞状态键（如 thumb:{userId}）
local userId = ARGV[1]            -- 用户 ID
local blogId = ARGV[2]            -- 博客 ID

-- 1. 检查用户是否已点赞（1-存在， 0-不存在）
if redis.call('HEXISTS', userThumbKey, blogId) == 0 then
    return -1  -- 未点赞，返回 -1 表示失败
end

-- 2. 获取当前临时计数（若不存在则默认为 0）
local hashKey = userId .. ':' .. blogId
local oldNumber = tonumber(redis.call('HGET', tempThumbKey, hashKey) or 0)

-- 3. 计算新值并更新
local newNumber = oldNumber - 1

-- 4. 原子性操作：更新临时计数 + 删除用户点赞标记
redis.call('HSET', tempThumbKey, hashKey, newNumber)
redis.call('HDEL', userThumbKey, blogId)

return 1  -- 返回 1 表示成功