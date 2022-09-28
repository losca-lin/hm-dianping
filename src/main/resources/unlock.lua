local id = redis.call('get',KEYS[1])
local threadId = ARGV[1]
if(id == threadId) then
    return redis.call('del',KEYS[1])
end
return 0