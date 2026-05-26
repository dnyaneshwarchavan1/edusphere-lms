package com.edusphere.lms.dto;

import com.edusphere.lms.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotNull Role role
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long id, String name, String email, Role role) {}
}
