package com.edusphere.lms.controller;

import com.edusphere.lms.dto.DashboardDtos.EnrollmentResponse;
import com.edusphere.lms.dto.FeatureDtos.CertificateGenerateRequest;
import com.edusphere.lms.dto.FeatureDtos.CertificateResponse;
import com.edusphere.lms.dto.FeatureDtos.CheckoutRequest;
import com.edusphere.lms.dto.FeatureDtos.PaymentCheckoutResponse;
import com.edusphere.lms.dto.FeatureDtos.PaymentFailureRequest;
import com.edusphere.lms.dto.FeatureDtos.PaymentHistoryResponse;
import com.edusphere.lms.dto.FeatureDtos.PaymentProviderConfigResponse;
import com.edusphere.lms.dto.FeatureDtos.RazorpayVerifyRequest;
import com.edusphere.lms.dto.QuizDtos.*;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.LearningService;
import com.edusphere.lms.service.PaymentService;
import com.edusphere.lms.service.QuizService;
import com.edusphere.lms.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {
    private final LearningService learningService;
    private final QuizService quizService;
    private final PaymentService paymentService;
    private final CertificateService certificateService;

    @PostMapping("/courses/{courseId}/enroll")
    public EnrollmentResponse enroll(@PathVariable Long courseId, @AuthenticationPrincipal User student) {
        return learningService.enroll(courseId, student);
    }

    @GetMapping("/enrollments")
    public List<EnrollmentResponse> enrollments(@AuthenticationPrincipal User student) {
        return learningService.myEnrollments(student);
    }

    @PatchMapping("/lessons/{lessonId}/complete")
    public EnrollmentResponse completeLesson(@PathVariable Long lessonId, @AuthenticationPrincipal User student) {
        return learningService.completeLesson(lessonId, student);
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public List<QuizResponse> quizzes(@PathVariable Long courseId) {
        return quizService.byCourse(courseId);
    }

    @PostMapping("/quizzes/{quizId}/submit")
    public QuizAttemptResponse submit(@PathVariable Long quizId, @RequestBody QuizSubmitRequest request, @AuthenticationPrincipal User student) {
        return quizService.submit(quizId, request, student);
    }

    @GetMapping("/quiz-attempts")
    public List<QuizAttemptResponse> attempts(@AuthenticationPrincipal User student) {
        return quizService.attempts(student);
    }

    @PostMapping("/courses/{courseId}/checkout")
    public PaymentCheckoutResponse checkout(@PathVariable Long courseId, @Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal User student) {
        return paymentService.createCheckout(courseId, request, student);
    }

    @GetMapping("/payments/razorpay-config")
    public PaymentProviderConfigResponse razorpayConfig() {
        return paymentService.razorpayConfig();
    }

    @PostMapping("/payments/{paymentId}/verify")
    public PaymentCheckoutResponse verifyPayment(@PathVariable Long paymentId, @Valid @RequestBody RazorpayVerifyRequest request, @AuthenticationPrincipal User student) {
        return paymentService.verifyCheckout(paymentId, request, student);
    }

    @PostMapping("/payments/{paymentId}/fail")
    public PaymentCheckoutResponse failPayment(@PathVariable Long paymentId, @RequestBody PaymentFailureRequest request, @AuthenticationPrincipal User student) {
        return paymentService.failCheckout(paymentId, request, student);
    }

    @GetMapping("/payments")
    public List<PaymentHistoryResponse> payments(@AuthenticationPrincipal User student) {
        return paymentService.history(student);
    }

    @GetMapping("/certificates")
    public List<CertificateResponse> certificates(@AuthenticationPrincipal User student) {
        return certificateService.myCertificates(student);
    }

    @PostMapping("/certificates/{courseId}")
    public CertificateResponse generateCertificate(@PathVariable Long courseId, @Valid @RequestBody CertificateGenerateRequest request, @AuthenticationPrincipal User student) {
        return certificateService.generate(courseId, request.studentFullName(), student);
    }

    @GetMapping("/certificates/{courseId}")
    public CertificateResponse certificate(@PathVariable Long courseId, @AuthenticationPrincipal User student) {
        return certificateService.get(courseId, student);
    }

    @GetMapping(value = "/certificates/{courseId}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long courseId, @AuthenticationPrincipal User student) {
        CertificateResponse certificate = certificateService.get(courseId, student);
        byte[] pdf = certificateService.download(courseId, student);
        String fileName = ("certificate-" + certificate.courseTitle()).replaceAll("[^a-zA-Z0-9-]+", "-").toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
