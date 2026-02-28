package com.example.Backend.controller;

import com.example.Backend.model.Users;
import com.example.Backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody Users user) {
        return authService.register(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody Users user) {
        return authService.login(user);
    }
}
