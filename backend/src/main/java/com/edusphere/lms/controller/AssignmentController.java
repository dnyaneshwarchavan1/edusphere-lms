package com.edusphere.lms.controller;

import com.edusphere.lms.dto.AssignmentDtos.*;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping(value = "/instructor/courses/{courseId}/assignments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssignmentResponse create(
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueAt,
            @RequestParam(required = false) Integer maxMarks,
            @RequestParam(required = false) MultipartFile attachment,
            @AuthenticationPrincipal User instructor
    ) {
        return assignmentService.create(courseId, title, description, dueAt, maxMarks, attachment, instructor);
    }

    @GetMapping("/instructor/courses/{courseId}/assignments")
    public List<AssignmentResponse> assignmentsForInstructor(@PathVariable Long courseId, @AuthenticationPrincipal User instructor) {
        return assignmentService.byCourseForInstructor(courseId, instructor);
    }

    @GetMapping("/student/courses/{courseId}/assignments")
    public List<AssignmentResponse> assignmentsForStudent(@PathVariable Long courseId, @AuthenticationPrincipal User student) {
        return assignmentService.byCourseForStudent(courseId, student);
    }

    @PostMapping(value = "/student/assignments/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionResponse submit(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String submissionText,
            @RequestParam(required = false) MultipartFile file,
            @AuthenticationPrincipal User student
    ) {
        return assignmentService.submit(assignmentId, submissionText, file, student);
    }

    @GetMapping("/student/assignments/submissions")
    public List<SubmissionResponse> mySubmissions(@AuthenticationPrincipal User student) {
        return assignmentService.submissionsForStudent(student);
    }

    @GetMapping("/instructor/assignments/{assignmentId}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long assignmentId, @AuthenticationPrincipal User instructor) {
        return assignmentService.submissionsForInstructor(assignmentId, instructor);
    }

    @PostMapping("/instructor/submissions/{submissionId}/grade")
    public SubmissionResponse grade(@PathVariable Long submissionId, @Valid @RequestBody GradeRequest request, @AuthenticationPrincipal User instructor) {
        return assignmentService.grade(submissionId, request, instructor);
    }
}
