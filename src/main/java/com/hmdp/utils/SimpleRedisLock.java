package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author Losca
 * @date 2022/9/26 13:21
 */
public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.fastUUID().toString(true)+"-";

    @Override
    public boolean tryLock(long time) {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, time, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        //获取当前线程名
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取redis锁
        String redisId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        //相同才可以删除，即只能释放自己的锁
        if (threadId.equals(redisId)){
            stringRedisTemplate.delete(KEY_PREFIX + name);

        }
    }
}
