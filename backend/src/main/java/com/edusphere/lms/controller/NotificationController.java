package com.edusphere.lms.controller;

import com.edusphere.lms.dto.FeatureDtos.NotificationResponse;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> recent(@AuthenticationPrincipal User user) {
        return notificationService.recent(user);
    }

    @GetMapping("/unread-count")
    public long unreadCount(@AuthenticationPrincipal User user) {
        return notificationService.unreadCount(user);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return notificationService.markRead(id, user);
    }
}
