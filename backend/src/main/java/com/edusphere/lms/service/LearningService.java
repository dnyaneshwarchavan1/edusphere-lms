package com.edusphere.lms.service;

import com.edusphere.lms.dto.DashboardDtos.AdminEnrollmentResponse;
import com.edusphere.lms.dto.DashboardDtos.EnrollmentResponse;
import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final NotificationService notificationService;
    private final CertificateService certificateService;

    @Transactional
    public EnrollmentResponse enroll(Long courseId, User student) {
        Course course = courseService.findCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Course is not published");
        }
        if (course.getPrice().signum() > 0 && !paymentRepository.existsByStudentAndCourseAndStatus(student, course, PaymentStatus.SUCCESS)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Complete payment before enrolling in this course");
        }
        return createEnrollment(course, student);
    }

    @Transactional
    public EnrollmentResponse enrollAfterPayment(Course course, User student) {
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            return toResponse(enrollmentRepository.findByStudentAndCourse(student, course)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Already enrolled")));
        }
        return createEnrollment(course, student);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myEnrollments(User student) {
        return enrollmentRepository.findByStudent(student).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> courseStudents(Long courseId) {
        Course course = courseService.findCourse(courseId);
        return enrollmentRepository.findByCourse(course).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public void requireEnrollment(Course course, User student) {
        if (!enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Enroll before accessing this course activity");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminEnrollmentResponse> allEnrollmentsForAdmin() {
        return enrollmentRepository.findAll().stream()
                .map(enrollment -> {
                    PaymentTransaction payment = paymentRepository.findTopByStudentAndCourseOrderByCreatedAtDesc(
                            enrollment.getStudent(), enrollment.getCourse()
                    ).orElse(null);
                    return new AdminEnrollmentResponse(
                            enrollment.getId(),
                            enrollment.getStudent().getName(),
                            enrollment.getStudent().getEmail(),
                            enrollment.getCourse().getId(),
                            enrollment.getCourse().getTitle(),
                            enrollment.getProgressPercent(),
                            payment == null ? "FREE" : payment.getStatus().name(),
                            payment == null ? "-" : payment.getReference(),
                            payment == null ? "FREE" : payment.getProvider()
                    );
                })
                .toList();
    }

    @Transactional
    public EnrollmentResponse completeLesson(Long lessonId, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lesson not found"));
        Course course = lesson.getModule().getCourse();
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Enroll before learning"));

        if (!progressRepository.existsByStudentAndLesson(student, lesson)) {
            progressRepository.save(LessonProgress.builder().student(student).lesson(lesson).build());
        }

        int totalLessons = course.getModules().stream().mapToInt(module -> module.getLessons().size()).sum();
        long completed = progressRepository.countByStudentAndLessonModuleCourseId(student, course.getId());
        int percent = totalLessons == 0 ? 0 : (int) Math.round((completed * 100.0) / totalLessons);
        enrollment.setProgressPercent(percent);
        Enrollment saved = enrollmentRepository.save(enrollment);
        certificateService.ensureCertificateIfEligible(saved);
        return toResponse(saved);
    }

    private EnrollmentResponse createEnrollment(Course course, User student) {
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new ApiException(HttpStatus.CONFLICT, "Already enrolled");
        }
        Enrollment enrollment = Enrollment.builder().student(student).course(course).progressPercent(0).build();
        Enrollment saved = enrollmentRepository.save(enrollment);
        notificationService.notifyUser(
                student,
                "Enrollment confirmed",
                "You are enrolled in " + course.getTitle() + ".",
                NotificationType.ENROLLMENT,
                "/courses/" + course.getId()
        );
        notificationService.notifyUser(
                course.getInstructor(),
                "New student enrolled",
                student.getName() + " enrolled in " + course.getTitle() + ".",
                NotificationType.ENROLLMENT,
                "/instructor"
        );
        return toResponse(saved);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(), enrollment.getCourse().getId(), enrollment.getCourse().getTitle(), enrollment.getProgressPercent());
    }
}
