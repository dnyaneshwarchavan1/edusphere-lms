package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.PaymentStatus;
import com.edusphere.lms.entity.PaymentTransaction;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByStudentOrderByCreatedAtDesc(User student);
    boolean existsByStudentAndCourseAndStatus(User student, Course course, PaymentStatus status);
    Optional<PaymentTransaction> findByIdAndStudent(Long id, User student);
    Optional<PaymentTransaction> findTopByStudentAndCourseOrderByCreatedAtDesc(User student, Course course);
    Optional<PaymentTransaction> findByProviderOrderId(String providerOrderId);
}
