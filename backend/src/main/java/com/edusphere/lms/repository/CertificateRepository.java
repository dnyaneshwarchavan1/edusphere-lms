package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Certificate;
import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudentOrderByIssuedAtDesc(User student);
    Optional<Certificate> findByStudentAndCourse(User student, Course course);
}
