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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "lesson_id"}))
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User student;

    @ManyToOne(optional = false)
    private Lesson lesson;

    @Column(nullable = false)
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        completedAt = Instant.now();
    }
}
