package com.example.testprojmesh.api.controller;

import com.example.testprojmesh.api.dto.LoginRequest;
import com.example.testprojmesh.api.dto.LoginResponse;
import com.example.testprojmesh.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(auth.login(request.identifier(), request.password()));
    }
}
