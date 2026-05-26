package com.edusphere.lms.dto;

import com.edusphere.lms.entity.CourseStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public class CourseDtos {
    public record CourseRequest(
            @NotBlank String title,
            @NotBlank String description,
            String category,
            String level,
            String thumbnailUrl,
            BigDecimal price
    ) {}

    public record ModuleRequest(@NotBlank String title, Integer position) {}

    public record LessonRequest(
            @NotBlank String title,
            String content,
            String videoUrl,
            String resourceUrl,
            Integer position
    ) {}

    public record CourseResponse(
            Long id,
            String title,
            String description,
            String category,
            String level,
            String thumbnailUrl,
            BigDecimal price,
            CourseStatus status,
            String instructorName,
            int moduleCount
    ) {}

    public record LessonResponse(Long id, String title, String content, String videoUrl, String resourceUrl, Integer position) {}
    public record ModuleResponse(Long id, String title, Integer position, List<LessonResponse> lessons) {}
    public record CourseDetailResponse(CourseResponse course, List<ModuleResponse> modules) {}
}
