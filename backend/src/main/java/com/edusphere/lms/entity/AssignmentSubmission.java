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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class AssignmentSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Assignment assignment;

    @ManyToOne(optional = false)
    private User student;

    @Column(length = 3000)
    private String submissionText;

    private String fileOriginalName;
    private String fileStoredName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    private Integer marks;

    @Column(length = 2000)
    private String feedback;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant gradedAt;

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
        if (status == null) {
            status = SubmissionStatus.SUBMITTED;
        }
    }
}
