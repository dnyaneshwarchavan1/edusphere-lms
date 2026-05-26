package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Lesson;
import com.edusphere.lms.entity.LessonProgress;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    boolean existsByStudentAndLesson(User student, Lesson lesson);
    long countByStudentAndLessonModuleCourseId(User student, Long courseId);
}
