package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
}
