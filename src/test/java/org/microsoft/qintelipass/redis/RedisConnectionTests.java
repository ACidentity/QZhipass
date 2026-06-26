package org.microsoft.qintelipass.redis;

import org.junit.jupiter.api.Test;
import org.microsoft.qintelipass.services.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisConnectionTests {
    @Autowired
    private RedisService redisService;

    @Test
    void testString() {
        redisService.setValue("name", "alec");
        Object name = redisService.getValue("name");
        System.out.println("name = " + name);
    }
}
