package com.edusphere.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class AuthHealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "auth-service", "status", "UP");
    }
}
