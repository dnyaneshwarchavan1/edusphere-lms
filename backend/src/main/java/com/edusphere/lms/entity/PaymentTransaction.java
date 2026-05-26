package com.edusphere.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User student;

    @ManyToOne(optional = false)
    private Course course;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "provider_order_id", unique = true)
    private String providerOrderId;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "failure_code")
    private String failureCode;

    private Instant paidAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (currency == null) {
            currency = "INR";
        }
        if (provider == null) {
            provider = "Razorpay";
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}
