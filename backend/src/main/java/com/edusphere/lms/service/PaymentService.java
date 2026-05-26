package com.edusphere.lms.service;

import com.edusphere.lms.dto.FeatureDtos.CheckoutRequest;
import com.edusphere.lms.dto.FeatureDtos.PaymentCheckoutResponse;
import com.edusphere.lms.dto.FeatureDtos.PaymentFailureRequest;
import com.edusphere.lms.dto.FeatureDtos.PaymentHistoryResponse;
import com.edusphere.lms.dto.FeatureDtos.PaymentProviderConfigResponse;
import com.edusphere.lms.dto.FeatureDtos.RazorpayVerifyRequest;
import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.CourseStatus;
import com.edusphere.lms.entity.NotificationType;
import com.edusphere.lms.entity.PaymentStatus;
import com.edusphere.lms.entity.PaymentTransaction;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final CourseService courseService;
    private final PaymentTransactionRepository paymentRepository;
    private final LearningService learningService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${app.razorpay-key-id:}")
    private String razorpayKeyId;

    @Value("${app.razorpay-key-secret:}")
    private String razorpayKeySecret;

    @Value("${app.razorpay-base-url:https://api.razorpay.com}")
    private String razorpayBaseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional(readOnly = true)
    public PaymentProviderConfigResponse razorpayConfig() {
        ensureRazorpayConfigured();
        return new PaymentProviderConfigResponse("Razorpay", razorpayKeyId, "INR");
    }

    @Transactional
    public PaymentCheckoutResponse createCheckout(Long courseId, CheckoutRequest request, User student) {
        ensureRazorpayConfigured();

        Course course = courseService.findCourse(courseId);
        validateCourseForPayment(course, student);

        long amountInSubunits = toSubunits(course.getPrice());
        String localReference = "EDU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String providerOrderId = createRazorpayOrder(amountInSubunits, course, student, localReference);

        PaymentTransaction payment = paymentRepository.save(PaymentTransaction.builder()
                .student(student)
                .course(course)
                .amount(course.getPrice())
                .reference(localReference)
                .providerOrderId(providerOrderId)
                .build());

        return toCheckoutResponse(payment, amountInSubunits, "Razorpay order created. Complete the payment to unlock enrollment.");
    }

    @Transactional
    public PaymentCheckoutResponse verifyCheckout(Long paymentId, RazorpayVerifyRequest request, User student) {
        PaymentTransaction payment = paymentRepository.findByIdAndStudent(paymentId, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toCheckoutResponse(payment, toSubunits(payment.getAmount()), "Payment already verified.");
        }

        if (!request.razorpayOrderId().equals(payment.getProviderOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay order does not match this payment.");
        }

        verifySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());

        PaymentGatewaySnapshot gatewayPayment = fetchRazorpayPayment(request.razorpayPaymentId());
        if (!request.razorpayOrderId().equals(gatewayPayment.orderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Gateway payment is linked to a different order.");
        }
        if (!"captured".equalsIgnoreCase(gatewayPayment.status()) && !"authorized".equalsIgnoreCase(gatewayPayment.status())) {
            markFailed(payment, gatewayPayment.errorCode(), "Razorpay status: " + gatewayPayment.status());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment is not successful yet. Current Razorpay status: " + gatewayPayment.status());
        }

        payment.setProviderPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        payment.setPaidAt(Instant.now());
        PaymentTransaction saved = paymentRepository.save(payment);

        learningService.enrollAfterPayment(saved.getCourse(), student);

        notificationService.notifyUser(
                student,
                "Payment successful",
                "Your Razorpay payment for " + saved.getCourse().getTitle() + " is confirmed.",
                NotificationType.PAYMENT,
                "/courses/" + saved.getCourse().getId()
        );
        notificationService.notifyUser(
                saved.getCourse().getInstructor(),
                "New paid enrollment",
                student.getName() + " completed a paid checkout for " + saved.getCourse().getTitle() + ".",
                NotificationType.PAYMENT,
                "/instructor"
        );

        return toCheckoutResponse(saved, toSubunits(saved.getAmount()), "Payment completed and enrollment activated.");
    }

    @Transactional
    public PaymentCheckoutResponse failCheckout(Long paymentId, PaymentFailureRequest request, User student) {
        PaymentTransaction payment = paymentRepository.findByIdAndStudent(paymentId, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toCheckoutResponse(payment, toSubunits(payment.getAmount()), "Payment already succeeded.");
        }

        if (request.razorpayOrderId() != null && payment.getProviderOrderId() != null
                && !payment.getProviderOrderId().equals(request.razorpayOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay order does not match this payment.");
        }

        if (request.razorpayPaymentId() != null && !request.razorpayPaymentId().isBlank()) {
            payment.setProviderPaymentId(request.razorpayPaymentId());
        }
        markFailed(payment, request.code(), buildFailureReason(request));

        return toCheckoutResponse(payment, toSubunits(payment.getAmount()), "Payment failed. Enrollment was not activated.");
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> history(User student) {
        return paymentRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentHistoryResponse latestFor(User student, Course course) {
        return paymentRepository.findTopByStudentAndCourseOrderByCreatedAtDesc(student, course)
                .map(this::toHistoryResponse)
                .orElse(null);
    }

    private void validateCourseForPayment(Course course, User student) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Course is not published");
        }
        if (course.getPrice().signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This course is free. Use direct enrollment.");
        }
        if (paymentRepository.existsByStudentAndCourseAndStatus(student, course, PaymentStatus.SUCCESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment already completed for this course");
        }
    }

    private void ensureRazorpayConfigured() {
        if (blank(razorpayKeyId) || blank(razorpayKeySecret)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Razorpay keys are missing. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in backend\\.env or system environment variables before starting payments.");
        }
    }

    private String createRazorpayOrder(long amountInSubunits, Course course, User student, String receipt) {
        try {
            String payload = objectMapper.writeValueAsString(new RazorpayOrderRequest(
                    amountInSubunits,
                    "INR",
                    receipt,
                    new RazorpayNotes(String.valueOf(course.getId()), String.valueOf(student.getId()))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(razorpayBaseUrl + "/v1/orders"))
                    .header("Authorization", basicAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 401 || response.body().contains("Authentication failed")) {
                    throw new ApiException(
                            HttpStatus.BAD_GATEWAY,
                            "Razorpay authentication failed. Check RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET, and make sure both keys are from the same Razorpay mode."
                    );
                }
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Razorpay order creation failed: " + summarizeGatewayError(response.body()));
            }

            JsonNode json = objectMapper.readTree(response.body());
            String orderId = json.path("id").asText();
            if (blank(orderId)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order creation returned an empty order id.");
            }
            return orderId;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to create Razorpay order: " + exception.getMessage());
        }
    }

    private PaymentGatewaySnapshot fetchRazorpayPayment(String paymentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(razorpayBaseUrl + "/v1/payments/" + paymentId))
                    .header("Authorization", basicAuthHeader())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Razorpay payment verification failed: " + summarizeGatewayError(response.body()));
            }

            JsonNode json = objectMapper.readTree(response.body());
            return new PaymentGatewaySnapshot(
                    json.path("id").asText(),
                    json.path("order_id").asText(),
                    json.path("status").asText(),
                    json.path("error_code").asText(null)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to verify Razorpay payment: " + exception.getMessage());
        }
    }

    private void verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = toHex(digest);
            if (!expected.equals(signature)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature.");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify Razorpay signature.");
        }
    }

    private void markFailed(PaymentTransaction payment, String failureCode, String failureReason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureCode(blank(failureCode) ? null : failureCode);
        payment.setFailureReason(blank(failureReason) ? "Payment failed." : failureReason);
        paymentRepository.save(payment);
    }

    private String buildFailureReason(PaymentFailureRequest request) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, request.description());
        appendPart(builder, request.reason());
        appendPart(builder, request.source());
        appendPart(builder, request.step());
        return builder.isEmpty() ? "Payment failed in Razorpay checkout." : builder.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (!blank(value)) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(value.trim());
        }
    }

    private String summarizeGatewayError(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode error = json.path("error");
            if (!error.isMissingNode()) {
                String code = error.path("code").asText("");
                String description = error.path("description").asText("");
                return (code + " " + description).trim();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private String basicAuthHeader() {
        String token = razorpayKeyId + ":" + razorpayKeySecret;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private long toSubunits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private PaymentCheckoutResponse toCheckoutResponse(PaymentTransaction payment, long amountInSubunits, String message) {
        return new PaymentCheckoutResponse(
                payment.getId(),
                payment.getCourse().getId(),
                payment.getCourse().getTitle(),
                payment.getAmount(),
                amountInSubunits,
                payment.getCurrency(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getReference(),
                payment.getStatus() == PaymentStatus.PENDING ? razorpayKeyId : null,
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getFailureReason(),
                message
        );
    }

    private PaymentHistoryResponse toHistoryResponse(PaymentTransaction payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getCourse().getId(),
                payment.getCourse().getTitle(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getReference(),
                payment.getProviderPaymentId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }

    private record RazorpayOrderRequest(
            long amount,
            String currency,
            String receipt,
            RazorpayNotes notes
    ) {}

    private record RazorpayNotes(
            String courseId,
            String studentId
    ) {}

    private record PaymentGatewaySnapshot(
            String paymentId,
            String orderId,
            String status,
            String errorCode
    ) {}
}
