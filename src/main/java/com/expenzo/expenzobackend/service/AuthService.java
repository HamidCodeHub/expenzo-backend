package com.expenzo.expenzobackend.service;

import com.expenzo.expenzobackend.dto.auth.JwtResponse;
import com.expenzo.expenzobackend.dto.auth.LoginRequest;
import com.expenzo.expenzobackend.dto.auth.RegisterRequest;
import com.expenzo.expenzobackend.dto.auth.UserDto;

public interface AuthService {

    JwtResponse login(LoginRequest loginRequest);
    UserDto register(RegisterRequest registerRequest);
}
