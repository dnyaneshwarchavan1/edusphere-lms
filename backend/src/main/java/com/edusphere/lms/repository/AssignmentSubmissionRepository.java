package com.edusphere.lms.repository;

import com.edusphere.lms.entity.Assignment;
import com.edusphere.lms.entity.AssignmentSubmission;
import com.edusphere.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Optional<AssignmentSubmission> findByAssignmentAndStudent(Assignment assignment, User student);
    List<AssignmentSubmission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);
    List<AssignmentSubmission> findByStudentOrderBySubmittedAtDesc(User student);
}
