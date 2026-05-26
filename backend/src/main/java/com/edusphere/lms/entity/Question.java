package com.edusphere.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private String correctAnswer;

    @ManyToOne(optional = false)
    private Quiz quiz;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "question_options")
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>();
}
