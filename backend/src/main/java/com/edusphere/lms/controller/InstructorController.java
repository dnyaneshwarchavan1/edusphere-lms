package com.edusphere.lms.controller;

import com.edusphere.lms.dto.CourseDtos.*;
import com.edusphere.lms.dto.DashboardDtos.EnrollmentResponse;
import com.edusphere.lms.dto.QuizDtos.QuizRequest;
import com.edusphere.lms.dto.QuizDtos.QuizResponse;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.CourseService;
import com.edusphere.lms.service.LearningService;
import com.edusphere.lms.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final QuizService quizService;

    @GetMapping("/courses")
    public List<CourseResponse> myCourses(@AuthenticationPrincipal User instructor) {
        return courseService.instructorCourses(instructor);
    }

    @PostMapping("/courses")
    public CourseResponse create(@Valid @RequestBody CourseRequest request, @AuthenticationPrincipal User instructor) {
        return courseService.create(request, instructor);
    }

    @PutMapping("/courses/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request, @AuthenticationPrincipal User instructor) {
        return courseService.update(id, request, instructor);
    }

    @PostMapping("/courses/{courseId}/modules")
    public ModuleResponse addModule(@PathVariable Long courseId, @Valid @RequestBody ModuleRequest request, @AuthenticationPrincipal User instructor) {
        return courseService.addModule(courseId, request, instructor);
    }

    @PostMapping("/modules/{moduleId}/lessons")
    public LessonResponse addLesson(@PathVariable Long moduleId, @Valid @RequestBody LessonRequest request, @AuthenticationPrincipal User instructor) {
        return courseService.addLesson(moduleId, request, instructor);
    }

    @GetMapping("/courses/{courseId}/students")
    public List<EnrollmentResponse> courseStudents(@PathVariable Long courseId) {
        return learningService.courseStudents(courseId);
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public List<QuizResponse> quizzes(@PathVariable Long courseId, @AuthenticationPrincipal User instructor) {
        return quizService.byCourseForInstructor(courseId, instructor);
    }

    @PostMapping("/courses/{courseId}/quizzes")
    public QuizResponse createQuiz(@PathVariable Long courseId, @Valid @RequestBody QuizRequest request, @AuthenticationPrincipal User instructor) {
        return quizService.create(courseId, request, instructor);
    }
}
