package com.edusphere.lms.controller;

import com.edusphere.lms.dto.AuthDtos.AuthResponse;
import com.edusphere.lms.dto.AuthDtos.LoginRequest;
import com.edusphere.lms.dto.AuthDtos.RegisterRequest;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal User user) {
        return authService.me(user);
    }
}
