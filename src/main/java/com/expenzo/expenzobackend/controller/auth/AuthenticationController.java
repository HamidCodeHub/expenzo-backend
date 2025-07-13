package com.expenzo.expenzobackend.controller.auth;

import com.expenzo.expenzobackend.dto.auth.JwtResponse;
import com.expenzo.expenzobackend.dto.auth.LoginRequest;
import com.expenzo.expenzobackend.dto.auth.RegisterRequest;
import com.expenzo.expenzobackend.dto.auth.UserDto;
import com.expenzo.expenzobackend.service.AuthService;
import com.expenzo.expenzobackend.service.AuthServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public JwtResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public UserDto register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}