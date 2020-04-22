package com.home.service.impl;

import com.home.framework.annotation.LQDService;
import com.home.service.UserService;

@LQDService
public class UserServiceImpl implements UserService {
    @Override
    public String getById(String id) {
        return "service receive value: " + id;
    }
}
