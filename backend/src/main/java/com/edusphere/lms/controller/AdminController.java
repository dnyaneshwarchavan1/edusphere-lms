package com.edusphere.lms.controller;

import com.edusphere.lms.dto.CourseDtos.CourseResponse;
import com.edusphere.lms.dto.DashboardDtos.AdminAnalyticsResponse;
import com.edusphere.lms.dto.DashboardDtos.AdminStats;
import com.edusphere.lms.dto.DashboardDtos.AdminEnrollmentResponse;
import com.edusphere.lms.dto.DashboardDtos.UserResponse;
import com.edusphere.lms.dto.QuizDtos.QuizResponse;
import com.edusphere.lms.repository.CourseRepository;
import com.edusphere.lms.repository.EnrollmentRepository;
import com.edusphere.lms.repository.UserRepository;
import com.edusphere.lms.service.AdminService;
import com.edusphere.lms.service.CourseService;
import com.edusphere.lms.service.LearningService;
import com.edusphere.lms.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final QuizService quizService;
    private final LearningService learningService;
    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminService.analytics().summary();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponse analytics() {
        return adminService.analytics();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isEnabled()))
                .toList();
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return courseService.allCourses();
    }

    @GetMapping("/enrollments")
    public List<AdminEnrollmentResponse> enrollments() {
        return learningService.allEnrollmentsForAdmin();
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public List<QuizResponse> quizzes(@PathVariable Long courseId) {
        return quizService.byCourse(courseId);
    }

    @GetMapping("/courses/{courseId}/quiz-count")
    public long quizCount(@PathVariable Long courseId) {
        return quizService.countByCourse(courseId);
    }

    @PatchMapping("/courses/{courseId}/approve")
    public CourseResponse approve(@PathVariable Long courseId) {
        return courseService.approve(courseId);
    }
}
