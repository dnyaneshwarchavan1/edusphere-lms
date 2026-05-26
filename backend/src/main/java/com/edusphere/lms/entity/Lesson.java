package com.edusphere.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String content;

    private String videoUrl;
    private String resourceUrl;

    @Column(nullable = false)
    private Integer position;

    @ManyToOne(optional = false)
    private CourseModule module;
}
