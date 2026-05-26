package com.edusphere.lms.service;

import com.edusphere.lms.dto.AssignmentDtos.*;
import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.AssignmentRepository;
import com.edusphere.lms.repository.AssignmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final CourseService courseService;
    private final LearningService learningService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentFileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional
    public AssignmentResponse create(Long courseId, String title, String description, Instant dueAt, Integer maxMarks, MultipartFile attachment, User instructor) {
        Course course = courseService.findCourse(courseId);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can edit only your course");
        }
        if (dueAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deadline is required");
        }
        Assignment saved = assignmentRepository.save(Assignment.builder()
                .course(course)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .maxMarks(maxMarks == null ? 100 : maxMarks)
                .attachmentOriginalName(attachment == null ? null : attachment.getOriginalFilename())
                .attachmentStoredName(fileStorageService.storeReferenceFile(attachment))
                .build());
        notificationService.notifyCourseStudents(
                course,
                "New assignment published",
                saved.getTitle() + " is now available in " + course.getTitle() + ".",
                "/courses/" + course.getId()
        );
        return toAssignmentResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> byCourseForInstructor(Long courseId, User instructor) {
        Course course = courseService.findCourse(courseId);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can view assignments only for your course");
        }
        return assignmentRepository.findByCourseOrderByDueAtAsc(course).stream()
                .map(assignment -> toAssignmentResponse(assignment, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> byCourseForStudent(Long courseId, User student) {
        Course course = courseService.findCourse(courseId);
        learningService.requireEnrollment(course, student);
        return assignmentRepository.findByCourseOrderByDueAtAsc(course).stream()
                .map(assignment -> toAssignmentResponse(assignment, submissionRepository.findByAssignmentAndStudent(assignment, student).orElse(null)))
                .toList();
    }

    @Transactional
    public SubmissionResponse submit(Long assignmentId, String submissionText, MultipartFile file, User student) {
        Assignment assignment = findAssignment(assignmentId);
        learningService.requireEnrollment(assignment.getCourse(), student);
        AssignmentSubmission submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
                .orElse(AssignmentSubmission.builder().assignment(assignment).student(student).build());
        submission.setSubmissionText(submissionText);
        if (file != null && !file.isEmpty()) {
            submission.setFileOriginalName(file.getOriginalFilename());
            submission.setFileStoredName(fileStorageService.storeSubmissionFile(file));
        }
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(assignment.getDueAt().isBefore(Instant.now()) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);
        AssignmentSubmission saved = submissionRepository.save(submission);
        notificationService.notifyUser(
                assignment.getCourse().getInstructor(),
                "Assignment submitted",
                student.getName() + " submitted " + assignment.getTitle() + ".",
                NotificationType.ENROLLMENT,
                "/instructor"
        );
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> submissionsForInstructor(Long assignmentId, User instructor) {
        Assignment assignment = findAssignment(assignmentId);
        if (!assignment.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can review submissions only for your course");
        }
        return submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeRequest request, User instructor) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        if (!submission.getAssignment().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can grade only your course submissions");
        }
        submission.setMarks(request.marks());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(Instant.now());
        AssignmentSubmission saved = submissionRepository.save(submission);
        notificationService.notifyUser(
                saved.getStudent(),
                "Assignment graded",
                saved.getAssignment().getTitle() + " has been graded with feedback.",
                NotificationType.QUIZ,
                "/courses/" + saved.getAssignment().getCourse().getId()
        );
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> submissionsForStudent(User student) {
        return submissionRepository.findByStudentOrderBySubmittedAtDesc(student).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    @Transactional(readOnly = true)
    public AssignmentSubmission findSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    public String referenceFileUrl(Assignment assignment) {
        return assignment.getAttachmentStoredName() == null ? null : "/api/files/assignments/reference/" + assignment.getId();
    }

    public String submissionFileUrl(AssignmentSubmission submission) {
        return submission.getFileStoredName() == null ? null : "/api/files/assignments/submission/" + submission.getId();
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment, AssignmentSubmission submission) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getTitle(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getMaxMarks(),
                assignment.getAttachmentOriginalName(),
                referenceFileUrl(assignment),
                submission == null ? null : toSubmissionSummary(submission)
        );
    }

    private SubmissionSummary toSubmissionSummary(AssignmentSubmission submission) {
        return new SubmissionSummary(
                submission.getId(),
                submission.getStatus(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getSubmittedAt(),
                submission.getFileOriginalName(),
                submissionFileUrl(submission),
                submission.getSubmissionText()
        );
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getStudent().getEmail(),
                submission.getStatus(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getSubmittedAt(),
                submission.getFileOriginalName(),
                submissionFileUrl(submission),
                submission.getSubmissionText()
        );
    }
}
