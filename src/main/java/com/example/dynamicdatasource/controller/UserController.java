package com.example.dynamicdatasource.controller;

import com.example.dynamicdatasource.entity.User;
import com.example.dynamicdatasource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /** GET /users —— 从从库（slave）查询全部用户 */
    @GetMapping
    public List<User> listUsers() {
        return userService.listUsers();
    }

    /** POST /users —— 向主库（master）写入用户 */
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}
