package org.microsoft.qintelipass.repositories;

import org.microsoft.qintelipass.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MySQL 实现 - UserRepository
 *
 * 使用 Spring Data JPA 自动实现 CRUD
 * 通过 UserJpaRepository 操作数据库
 *
 * 启用方式：在 application.properties 中配置好 MySQL 后，
 * 移除此类上 @Primary 的注释，或将 RedisUserRepositoryImpl 上的 @Primary 移除
 */
@Repository
@Primary  // 启用 MySQL 作为数据存储
public class MySQLUserRepositoryImpl implements UserRepository {

    @Autowired(required = false)
    private UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userJpaRepository.findByPhone(phone);
    }

    @Override
    public Optional<User> findByWechat(String wechat) {
        return userJpaRepository.findByWechat(wechat);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public void deleteById(Long userId) {
        userJpaRepository.deleteById(userId);
    }

    @Override
    public boolean existsById(Long userId) {
        return userJpaRepository.existsById(userId);
    }

    @Override
    public long count() {
        return userJpaRepository.count();
    }
}
