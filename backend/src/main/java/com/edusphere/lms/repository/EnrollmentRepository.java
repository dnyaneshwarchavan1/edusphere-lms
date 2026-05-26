package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.Enrollment;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentAndCourse(User student, Course course);
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    List<Enrollment> findByStudent(User student);
    List<Enrollment> findByCourse(Course course);
}
