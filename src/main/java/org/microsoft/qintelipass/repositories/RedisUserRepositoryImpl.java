package org.microsoft.qintelipass.repositories;

import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.enums.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis 实现 - UserRepository
 *
 * 使用方式：
 * 1. MVP 阶段：直接使用此实现
 * 2. 生产阶段：创建 MySQLUserRepositoryImpl，修改配置即可切换
 */
@Repository
// @Primary  // MVP阶段启用；切换到MySQL时注释掉
public class RedisUserRepositoryImpl implements UserRepository {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String USER_KEY_PREFIX = "user:";
    private static final String PHONE_INDEX_PREFIX = "user:phone:";
    private static final String WECHAT_INDEX_PREFIX = "user:wechat:";

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Optional<User> findById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String key = USER_KEY_PREFIX + userId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToUser(userId, data));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }

        String userIdStr = redisTemplate.opsForValue().get(PHONE_INDEX_PREFIX + phone);
        if (userIdStr == null) {
            return Optional.empty();
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            return findById(userId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByWechat(String wechat) {
        if (wechat == null || wechat.trim().isEmpty()) {
            return Optional.empty();
        }

        String userIdStr = redisTemplate.opsForValue().get(WECHAT_INDEX_PREFIX + wechat);
        if (userIdStr == null) {
            return Optional.empty();
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            return findById(userId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        var keys = redisTemplate.keys(USER_KEY_PREFIX + "*");

        if (keys != null) {
            for (String key : keys) {
                if (redisTemplate.type(key) != null &&
                    redisTemplate.type(key).name().equals("HASH")) {

                    Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                    String userIdStr = key.replace(USER_KEY_PREFIX, "");
                    try {
                        Long userId = Long.parseLong(userIdStr);
                        users.add(mapToUser(userId, data));
                    } catch (NumberFormatException e) {
                        // 跳过无效的 key
                    }
                }
            }
        }

        return users;
    }

    @Override
    public User save(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }

        String key = USER_KEY_PREFIX + user.getId();

        // 保存用户数据
        redisTemplate.opsForHash().put(key, "name",
            user.getName() != null ? user.getName() : "");
        redisTemplate.opsForHash().put(key, "phone",
            user.getPhone() != null ? user.getPhone() : "");
        redisTemplate.opsForHash().put(key, "password",
            user.getPassword() != null ? user.getPassword() : "");
        redisTemplate.opsForHash().put(key, "department",
            user.getDepartment() != null ? user.getDepartment() : "");
        redisTemplate.opsForHash().put(key, "email",
            user.getEmail() != null ? user.getEmail() : "");
        redisTemplate.opsForHash().put(key, "wechat",
            user.getWechat() != null ? user.getWechat() : "");
        redisTemplate.opsForHash().put(key, "status",
            user.getStatus() != null ? user.getStatus().name() : UserStatus.NORMAL.name());
        redisTemplate.opsForHash().put(key, "restored",
            user.getRestored() != null ? user.getRestored().toString() : "false");

        if (user.getCreatedAt() != null) {
            redisTemplate.opsForHash().put(key, "createdAt", user.getCreatedAt().format(DTF));
        }
        if (user.getUpdatedAt() != null) {
            redisTemplate.opsForHash().put(key, "updatedAt", user.getUpdatedAt().format(DTF));
        }
        if (user.getCancelledAt() != null) {
            redisTemplate.opsForHash().put(key, "cancelledAt", user.getCancelledAt().format(DTF));
        }

        // 建立索引
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            redisTemplate.opsForValue().set(PHONE_INDEX_PREFIX + user.getPhone(), user.getId().toString());
        }
        if (user.getWechat() != null && !user.getWechat().isEmpty()) {
            redisTemplate.opsForValue().set(WECHAT_INDEX_PREFIX + user.getWechat(), user.getId().toString());
        }

        return user;
    }

    @Override
    public void deleteById(Long userId) {
        if (userId == null) {
            return;
        }

        // 先查询用户，获取 phone 和 wechat
        Optional<User> userOpt = findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 删除索引
            if (user.getPhone() != null) {
                redisTemplate.delete(PHONE_INDEX_PREFIX + user.getPhone());
            }
            if (user.getWechat() != null) {
                redisTemplate.delete(WECHAT_INDEX_PREFIX + user.getWechat());
            }
        }

        // 删除用户数据
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    @Override
    public boolean existsById(Long userId) {
        if (userId == null) {
            return false;
        }

        return redisTemplate.hasKey(USER_KEY_PREFIX + userId);
    }

    @Override
    public long count() {
        var keys = redisTemplate.keys(USER_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }

    /**
     * 将 Redis Hash 转换为 User 对象
     */
    private User mapToUser(Long userId, Map<Object, Object> data) {
        User user = new User();
        user.setId(userId);
        user.setName((String) data.get("name"));
        user.setPhone((String) data.get("phone"));
        user.setPassword((String) data.get("password"));
        user.setDepartment((String) data.get("department"));
        user.setEmail((String) data.get("email"));
        user.setWechat((String) data.get("wechat"));

        String statusStr = (String) data.get("status");
        if (statusStr != null) {
            try {
                user.setStatus(UserStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                user.setStatus(UserStatus.NORMAL);
            }
        } else {
            user.setStatus(UserStatus.NORMAL);
        }

        String restoredStr = (String) data.get("restored");
        user.setRestored(restoredStr != null ? Boolean.parseBoolean(restoredStr) : false);

        String createdAtStr = (String) data.get("createdAt");
        if (createdAtStr != null) {
            user.setCreatedAt(LocalDateTime.parse(createdAtStr, DTF));
        }

        String updatedAtStr = (String) data.get("updatedAt");
        if (updatedAtStr != null) {
            user.setUpdatedAt(LocalDateTime.parse(updatedAtStr, DTF));
        }

        String cancelledAtStr = (String) data.get("cancelledAt");
        if (cancelledAtStr != null) {
            user.setCancelledAt(LocalDateTime.parse(cancelledAtStr, DTF));
        }

        return user;
    }
}
