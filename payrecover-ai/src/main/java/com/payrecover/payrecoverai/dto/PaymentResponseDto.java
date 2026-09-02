package com.payrecover.payrecoverai.dto;

import com.payrecover.payrecoverai.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO = Data Transfer Object.
 *
 * Why not just return the Payment @Entity directly from the controller?
 * 1. It decouples our API "contract" (what the frontend sees) from our
 *    database structure (what's actually stored). We can change the entity
 *    later (e.g. rename a column) without breaking the React app.
 * 2. It avoids accidentally leaking internal fields (like the numeric
 *    primary key "id") or triggering lazy-loading/serialization issues.
 * 3. In later phases we will ADD fields here (failureCategory, aiRecommendation,
 *    confidence, recoveryStatus) that don't exist on the entity at all --
 *    they get computed by other services and merged into this DTO.
 */
public class PaymentResponseDto {

    private String paymentId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String provider;
    private String failureCode;
    private Integer attempts;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public PaymentResponseDto() {
    }

    // ===== Getters and Setters =====

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
