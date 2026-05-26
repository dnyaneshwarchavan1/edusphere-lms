package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.CourseStatus;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatus(CourseStatus status);
    List<Course> findByInstructor(User instructor);
}
