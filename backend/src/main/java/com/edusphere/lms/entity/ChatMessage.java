package com.edusphere.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User sender;

    @ManyToOne(optional = false)
    private User recipient;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        read = false;
    }
}
