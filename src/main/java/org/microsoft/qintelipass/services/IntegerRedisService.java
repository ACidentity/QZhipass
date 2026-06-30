package org.microsoft.qintelipass.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IntegerRedisService implements IRedisService<Integer>{
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    @Override
    public void setValue(String key, Integer value) {
        redisTemplate.opsForValue().set(key, value);
    }
    @Override
    public Integer getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    @Override
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
