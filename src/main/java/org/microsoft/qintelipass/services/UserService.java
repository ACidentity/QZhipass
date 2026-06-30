package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.enums.UserStatus;
import org.microsoft.qintelipass.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String USER_KEY_PREFIX = "user:";
    private static final String PHONE_INDEX_PREFIX = "user:phone:";
    private static final String WECHAT_INDEX_PREFIX = "user:wechat:";

    public User getUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        String key = USER_KEY_PREFIX + userId;
        Map<String, String> data = (Map<String, String>) (Map<?, ?>) redisTemplate.opsForHash().entries(key);
        if (data == null || data.isEmpty()) {
            return null;
        }
        return mapToUser(userId, data);
    }

    public User getUserByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(PHONE_INDEX_PREFIX + phone);
        if (userId == null) {
            return null;
        }
        return getUserById(userId);
    }

    public User getUserByWechatOpenId(String wechatOpenId) {
        if (wechatOpenId == null || wechatOpenId.trim().isEmpty()) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(WECHAT_INDEX_PREFIX + wechatOpenId);
        if (userId == null) {
            return null;
        }
        return getUserById(userId);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        var keys = redisTemplate.keys(USER_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                if (redisTemplate.type(key) != null && redisTemplate.type(key).name().equals("HASH")) {
                    Map<String, String> data = (Map<String, String>) (Map<?, ?>) redisTemplate.opsForHash().entries(key);
                    String userId = key.replace(USER_KEY_PREFIX, "");
                    users.add(mapToUser(userId, data));
                }
            }
        }
        return users;
    }

    public void saveUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String key = USER_KEY_PREFIX + user.getId();
        redisTemplate.opsForHash().put(key, "phone", user.getPhone() != null ? user.getPhone() : "");
        redisTemplate.opsForHash().put(key, "wechatOpenId", user.getWechatOpenId() != null ? user.getWechatOpenId() : "");
        redisTemplate.opsForHash().put(key, "status", user.getStatus() != null ? user.getStatus().name() : UserStatus.NORMAL.name());
        redisTemplate.opsForHash().put(key, "name", user.getName() != null ? user.getName() : "");
        
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            redisTemplate.opsForValue().set(PHONE_INDEX_PREFIX + user.getPhone(), user.getId());
        }
        if (user.getWechatOpenId() != null && !user.getWechatOpenId().isEmpty()) {
            redisTemplate.opsForValue().set(WECHAT_INDEX_PREFIX + user.getWechatOpenId(), user.getId());
        }
    }

    public boolean deactivateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }
        if (UserStatus.DEACTIVATED.equals(user.getStatus())) {
            return false;
        }
        user.setStatus(UserStatus.DEACTIVATED);
        saveUser(user);
        return true;
    }

    public boolean isUserDeactivated(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        User user = getUserById(userId);
        return user != null && UserStatus.DEACTIVATED.equals(user.getStatus());
    }

    private User mapToUser(String userId, Map<String, String> data) {
        User user = new User();
        user.setId(userId);
        user.setPhone(data.get("phone"));
        user.setWechatOpenId(data.get("wechatOpenId"));
        
        String statusStr = data.get("status");
        if (statusStr != null) {
            try {
                user.setStatus(UserStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                user.setStatus(UserStatus.NORMAL);
            }
        } else {
            user.setStatus(UserStatus.NORMAL);
        }
        
        user.setName(data.get("name"));
        return user;
    }
}
