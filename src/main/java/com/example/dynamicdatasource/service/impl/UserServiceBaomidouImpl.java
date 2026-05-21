package com.example.dynamicdatasource.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dynamicdatasource.entity.User;
import com.example.dynamicdatasource.mapper.UserMapper;
import com.example.dynamicdatasource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 方案一：使用 Baomidou 官方 dynamic-datasource starter
 * 只需 @DS 注解声明数据源名称即可完成切换
 */
@Service
@Profile("baomidou")
public class UserServiceBaomidouImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @DS("slave")  // 从库读取
    public List<User> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
    }

    @Override
    @DS("master")  // 主库写入
    public User createUser(User user) {
        userMapper.insert(user);
        return user;
    }
}
