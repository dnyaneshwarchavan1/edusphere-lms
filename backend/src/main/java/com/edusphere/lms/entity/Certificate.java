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
@Table(name = "certificates", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User student;

    @ManyToOne(optional = false)
    private Course course;

    @Column(nullable = false, unique = true, length = 64)
    private String certificateNumber;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private Instant issuedAt;

    @PrePersist
    void onCreate() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
