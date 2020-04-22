package com.home.controller;

import com.home.framework.annotation.LQDAutowired;
import com.home.framework.annotation.LQDController;
import com.home.framework.annotation.LQDRequestMapping;
import com.home.service.UserService;

@LQDController
@LQDRequestMapping("/user")
public class UserController {

    @LQDAutowired
    private UserService userService;

    @LQDRequestMapping("/getById")
    public String getById(String id) {
        return userService.getById(id);
    }
}
