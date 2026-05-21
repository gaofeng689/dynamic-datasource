package com.example.dynamicdatasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dynamicdatasource.custom.annotation.DS;
import com.example.dynamicdatasource.entity.User;
import com.example.dynamicdatasource.mapper.UserMapper;
import com.example.dynamicdatasource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 方案二：使用自定义动态数据源实现
 * 使用自定义 @DS 注解 + AOP 切面完成切换
 */
@Service
@Profile("custom")
public class UserServiceCustomImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @DS("slave")  // 自定义注解 —— 由 AOP 切面拦截处理
    public List<User> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
    }

    @Override
    @DS("master")  // 自定义注解 —— 由 AOP 切面拦截处理
    public User createUser(User user) {
        userMapper.insert(user);
        return user;
    }
}
