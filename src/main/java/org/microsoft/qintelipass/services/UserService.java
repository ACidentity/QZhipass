package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    User getUserById(Long userId);
    User getUserByPhone(String phone);
    User getUserByWechatOpenId(String wechatOpenId);
    List<User> getAllUsers();
    void saveUser(User user);
    boolean deactivateUser(Long userId);
    boolean isUserDeactivated(Long userId);
    User findByUsername(String username);

    @Autowired
    private UserRepository userRepository;  // 依赖接口，不关心具体实现

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    public User getUserByWechat(String wechat) {
        return userRepository.findByWechat(wechat).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public boolean cancelUser(Long userId) {
        if (userId == null) {
            return false;
        }

        User user = getUserById(userId);
        if (user == null) {
            return false;
        }

        if (user.getStatus() != null && 
            UserStatus.CANCELLED.equals(user.getStatus())) {
            return false;  // 已经注销
        }

        // 修改状态并保存
        user.setStatus(UserStatus.CANCELLED);
        user.setCancelledAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        return true;
    }

    public boolean isUserCancelled(Long userId) {
        if (userId == null) {
            return false;
        }

        User user = getUserById(userId);
        return user != null && 
               user.getStatus() != null && 
               UserStatus.CANCELLED.equals(user.getStatus());
    }
}
