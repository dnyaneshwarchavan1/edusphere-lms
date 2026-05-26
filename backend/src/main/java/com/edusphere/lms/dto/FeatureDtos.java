package com.edusphere.lms.dto;

import com.edusphere.lms.entity.NotificationType;
import com.edusphere.lms.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public class FeatureDtos {
    public record CheckoutRequest() {}

    public record RazorpayVerifyRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {}

    public record PaymentFailureRequest(
            String razorpayOrderId,
            String razorpayPaymentId,
            String code,
            String description,
            String source,
            String step,
            String reason
    ) {}

    public record PaymentProviderConfigResponse(
            String provider,
            String keyId,
            String currency
    ) {}

    public record PaymentCheckoutResponse(
            Long paymentId,
            Long courseId,
            String courseTitle,
            BigDecimal amount,
            Long amountInSubunits,
            String currency,
            String provider,
            PaymentStatus status,
            String reference,
            String razorpayKeyId,
            String razorpayOrderId,
            String paymentIdReference,
            String failureReason,
            String message
    ) {}

    public record PaymentHistoryResponse(
            Long id,
            Long courseId,
            String courseTitle,
            BigDecimal amount,
            String currency,
            String provider,
            PaymentStatus status,
            String reference,
            String paymentIdReference,
            String failureReason,
            Instant createdAt,
            Instant paidAt
    ) {}

    public record CertificateGenerateRequest(
            @NotBlank String studentFullName
    ) {}

    public record CertificateResponse(
            Long id,
            Long courseId,
            String courseTitle,
            String studentFullName,
            String certificateNumber,
            int progressPercent,
            Instant issuedAt
    ) {}

    public record NotificationResponse(
            Long id,
            String title,
            String message,
            NotificationType type,
            String link,
            boolean read,
            Instant createdAt
    ) {}
}
