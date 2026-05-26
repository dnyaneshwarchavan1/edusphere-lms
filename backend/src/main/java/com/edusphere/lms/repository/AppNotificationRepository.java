package com.edusphere.lms.repository;

import com.edusphere.lms.entity.AppNotification;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop20ByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadFalse(User recipient);
    Optional<AppNotification> findByIdAndRecipient(Long id, User recipient);
}
