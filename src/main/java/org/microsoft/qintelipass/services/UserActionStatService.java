package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.ITrafficStatService;
import org.microsoft.qintelipass.util.ExpirationTimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserActionStatService implements ITrafficStatService {
    @Autowired
    private IntegerRedisService redisService;
    private static final String PREFIX = "user:traffic:";
    private static final String ACTIVE_COUNT_PREFIX = "user:active:count";
    @Override
    public void recordTraffic(Long userId) {
        String key = PREFIX + userId;
        redisService.getRedisTemplate().opsForValue().increment(key, 1);
        redisService.getRedisTemplate().expireAt(key, ExpirationTimeHelper.getNextDayTime());
        if (64 < redisService.getValue(key)){
            redisService.setValue(ACTIVE_COUNT_PREFIX, 1);
        }
    }

    @Override
    public void resetTraffic(Long userId) {
        String key = PREFIX + userId;
        redisService.setValue(key, 0);
    }

    @Override
    public List<Long> getAllActiveUsers() {
        return null;
    }

    @Override
    public Integer getActiveUsers() {
        return this.redisService.getValue(ACTIVE_COUNT_PREFIX);
    }
}
