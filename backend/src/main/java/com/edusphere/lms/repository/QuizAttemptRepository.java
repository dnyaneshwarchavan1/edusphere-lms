package com.edusphere.lms.repository;

import com.edusphere.lms.entity.QuizAttempt;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudent(User student);
}
