package com.edusphere.lms.dto;

import com.edusphere.lms.entity.SubmissionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class AssignmentDtos {
    public record AssignmentResponse(
            Long id,
            Long courseId,
            String courseTitle,
            String title,
            String description,
            Instant dueAt,
            Integer maxMarks,
            String attachmentName,
            String attachmentUrl,
            SubmissionSummary submission
    ) {}

    public record SubmissionSummary(
            Long id,
            SubmissionStatus status,
            Integer marks,
            String feedback,
            Instant submittedAt,
            String fileName,
            String fileUrl,
            String submissionText
    ) {}

    public record SubmissionResponse(
            Long id,
            Long assignmentId,
            String assignmentTitle,
            Long studentId,
            String studentName,
            String studentEmail,
            SubmissionStatus status,
            Integer marks,
            String feedback,
            Instant submittedAt,
            String fileName,
            String fileUrl,
            String submissionText
    ) {}

    public record GradeRequest(
            @NotNull @Min(0) @Max(1000) Integer marks,
            @NotBlank String feedback
    ) {}
}
