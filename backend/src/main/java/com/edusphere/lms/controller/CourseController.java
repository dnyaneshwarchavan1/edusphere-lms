package com.edusphere.lms.controller;

import com.edusphere.lms.dto.CourseDtos.CourseDetailResponse;
import com.edusphere.lms.dto.CourseDtos.CourseResponse;
import com.edusphere.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    public List<CourseResponse> published() {
        return courseService.publishedCourses();
    }

    @GetMapping("/{id}")
    public CourseDetailResponse detail(@PathVariable Long id) {
        return courseService.detail(id);
    }
}
