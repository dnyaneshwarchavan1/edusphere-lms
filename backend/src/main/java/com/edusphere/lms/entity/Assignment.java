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
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private Integer maxMarks;

    private String attachmentOriginalName;
    private String attachmentStoredName;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (maxMarks == null) {
            maxMarks = 100;
        }
    }
}
