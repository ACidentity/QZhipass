package org.microsoft.qintelipass.repositories;

import org.microsoft.qintelipass.models.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 *
 * 设计原则：
 * 1. 不依赖具体存储实现（Redis、MySQL、MongoDB 等）
 * 2. 业务层只依赖此接口，不关心数据存在哪里
 * 3. 后期切换存储实现时，业务代码不需要修改
 *
 * 使用方式：
 * - MVP 阶段：使用 RedisUserRepositoryImpl
 * - 生产阶段：切换到 MySQLUserRepositoryImpl
 */
public interface UserRepository {

    /**
     * 根据 ID 查询用户
     */
    Optional<User> findById(Long userId);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据微信号查询用户
     */
    Optional<User> findByWechat(String wechat);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 保存用户（新增或更新）
     */
    User save(User user);

    /**
     * 根据 ID 删除用户
     */
    void deleteById(Long userId);

    /**
     * 检查用户是否存在
     */
    boolean existsById(Long userId);

    /**
     * 统计用户总数
     */
    long count();
}
