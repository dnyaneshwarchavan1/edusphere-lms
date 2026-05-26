package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Assignment;
import com.edusphere.lms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseOrderByDueAtAsc(Course course);
}
