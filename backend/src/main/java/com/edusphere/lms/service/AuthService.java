package com.edusphere.lms.service;

import com.edusphere.lms.dto.AuthDtos.AuthResponse;
import com.edusphere.lms.dto.AuthDtos.LoginRequest;
import com.edusphere.lms.dto.AuthDtos.RegisterRequest;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.UserRepository;
import com.edusphere.lms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();
        userRepository.save(user);
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return authResponse(user);
    }

    public AuthResponse me(User user) {
        return authResponse(user);
    }

    private AuthResponse authResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
