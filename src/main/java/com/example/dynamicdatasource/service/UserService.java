package com.example.dynamicdatasource.service;

import com.example.dynamicdatasource.entity.User;

import java.util.List;

public interface UserService {

    /** 查询全部用户 —— 走从库 */
    List<User> listUsers();

    /** 新增用户 —— 走主库 */
    User createUser(User user);
}
