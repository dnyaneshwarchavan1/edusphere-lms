package com.edusphere.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    private String category;
    private String level;
    private String thumbnailUrl;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @ManyToOne(optional = false)
    private User instructor;

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CourseModule> modules = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = CourseStatus.PENDING;
        }
        if (price == null) {
            price = BigDecimal.ZERO;
        }
    }
}
