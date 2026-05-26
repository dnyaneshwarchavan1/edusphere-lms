package com.edusphere.lms.controller;

import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.service.AssignmentFileStorageService;
import com.edusphere.lms.service.AssignmentService;
import com.edusphere.lms.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files/assignments")
@RequiredArgsConstructor
public class AssignmentFileController {
    private final AssignmentService assignmentService;
    private final AssignmentFileStorageService fileStorageService;
    private final LearningService learningService;

    @GetMapping("/reference/{assignmentId}")
    public ResponseEntity<?> reference(@PathVariable Long assignmentId, @AuthenticationPrincipal User user) throws MalformedURLException {
        Assignment assignment = assignmentService.findAssignment(assignmentId);
        ensureReferenceAccess(assignment, user);
        return fileResponse(assignment.getAttachmentStoredName(), assignment.getAttachmentOriginalName());
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<?> submission(@PathVariable Long submissionId, @AuthenticationPrincipal User user) throws MalformedURLException {
        AssignmentSubmission submission = assignmentService.findSubmission(submissionId);
        ensureSubmissionAccess(submission, user);
        return fileResponse(submission.getFileStoredName(), submission.getFileOriginalName());
    }

    private void ensureReferenceAccess(Assignment assignment, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.INSTRUCTOR && assignment.getCourse().getInstructor().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.STUDENT) {
            learningService.requireEnrollment(assignment.getCourse(), user);
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this file");
    }

    private void ensureSubmissionAccess(AssignmentSubmission submission, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.INSTRUCTOR && submission.getAssignment().getCourse().getInstructor().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.STUDENT && submission.getStudent().getId().equals(user.getId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this file");
    }

    private ResponseEntity<UrlResource> fileResponse(Path file, String originalName) throws MalformedURLException {
        UrlResource resource = new UrlResource(file.toUri());
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (originalName == null ? "file" : originalName) + "\"")
                .body(resource);
    }

    private ResponseEntity<?> fileResponse(String storedName, String originalName) throws MalformedURLException {
        if (storedName == null || storedName.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }
        if (fileStorageService.isRemote(storedName)) {
            return ResponseEntity.status(302)
                    .location(URI.create(storedName))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (originalName == null ? "file" : originalName) + "\"")
                    .build();
        }
        return fileResponse(fileStorageService.resolve(storedName), originalName);
    }
}
