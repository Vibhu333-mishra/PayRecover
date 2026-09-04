package com.payrecover.payrecoverai.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * This class represents ONE ROW in the "payments" MySQL table.
 * Every field below becomes a column. Hibernate (the JPA implementation
 * Spring Boot uses) reads these annotations and creates the table for us.
 */
@Entity
@Table(name = "payments")
public class Payment {

    // Internal numeric primary key. Auto-incremented by MySQL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business-facing payment identifier, e.g. "PAY1001". This is what the
    // user sees on screen and what we use in API URLs like /api/payments/PAY1001
    @Column(name = "payment_id", nullable = false, unique = true, length = 30)
    private String paymentId;

    @Column(name = "customer_id", nullable = false, length = 30)
    private String customerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // UPI, CARD, NETBANKING, WALLET
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    // Bank / payment provider, e.g. "HDFC Bank", "Razorpay Gateway"
    @Column(name = "provider", length = 50)
    private String provider;

    // Raw failure code from the (simulated) payment gateway, e.g. "BANK_TIMEOUT".
    // Null when the payment succeeded on the first attempt.
    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(nullable = false)
    private Integer attempts;

    // Stored as a readable string ("FAILED", "RECOVERED", ...) instead of a number,
    // which makes the raw database table easy to read while debugging.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA requires a no-argument constructor.
    public Payment() {
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
