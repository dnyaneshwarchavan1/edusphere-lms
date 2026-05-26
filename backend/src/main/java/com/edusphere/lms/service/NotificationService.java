package com.edusphere.lms.service;

import com.edusphere.lms.dto.FeatureDtos.NotificationResponse;
import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.realtime.NotificationWebSocketHandler;
import com.edusphere.lms.repository.AppNotificationRepository;
import com.edusphere.lms.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final AppNotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Transactional
    public NotificationResponse notifyUser(User recipient, String title, String message, NotificationType type, String link) {
        AppNotification notification = notificationRepository.save(AppNotification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .build());
        NotificationResponse response = toResponse(notification);
        notificationWebSocketHandler.sendToUser(recipient.getId(), response);
        return response;
    }

    @Transactional
    public void notifyCourseStudents(Course course, String title, String message, String link) {
        enrollmentRepository.findByCourse(course).forEach(enrollment ->
                notifyUser(enrollment.getStudent(), title, message, NotificationType.QUIZ, link));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent(User user) {
        return notificationRepository.findTop20ByRecipientOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public NotificationResponse markRead(Long id, User user) {
        AppNotification notification = notificationRepository.findByIdAndRecipient(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
