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

    // ===== Added in Phase 5 =====
    // These come from the ai_diagnoses table, not from the payments table, and
    // they fill the "AI Recommendation" column of the Failed Payments screen.
    // All of them are null until the payment has been analysed at least once,
    // which is why `analyzed` exists: the UI checks that single boolean instead
    // of null-checking four fields.

    private boolean analyzed;
    private String failureCategory;       // e.g. "BANK_TIMEOUT"
    private String failureCategoryLabel;  // e.g. "Bank Timeout"
    private String aiRecommendation;      // e.g. "RETRY"
    private String aiRecommendationLabel; // e.g. "Retry Now"
    private Integer confidencePercent;    // e.g. 91
    private String aiSource;              // "LLM" or "FALLBACK_RULES"

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

    // ===== Phase 5 AI fields =====
    // Note the getter for a boolean is "isAnalyzed", not "getAnalyzed".
    // Jackson (the library that turns this object into JSON) uses that naming
    // rule to decide the JSON key, so the React app will see "analyzed": true.

    public boolean isAnalyzed() {
        return analyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getFailureCategoryLabel() {
        return failureCategoryLabel;
    }

    public void setFailureCategoryLabel(String failureCategoryLabel) {
        this.failureCategoryLabel = failureCategoryLabel;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public String getAiRecommendationLabel() {
        return aiRecommendationLabel;
    }

    public void setAiRecommendationLabel(String aiRecommendationLabel) {
        this.aiRecommendationLabel = aiRecommendationLabel;
    }

    public Integer getConfidencePercent() {
        return confidencePercent;
    }

    public void setConfidencePercent(Integer confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

    public String getAiSource() {
        return aiSource;
    }

    public void setAiSource(String aiSource) {
        this.aiSource = aiSource;
    }

    // ===== Added in Phase 6 =====
    // Policy Engine decision verdict fields
    private String policyDecision;       // "ALLOWED", "BLOCKED", "ESCALATED"
    private String policyDecisionLabel;  // "Recovery Allowed", etc.
    private String finalAction;          // "RETRY", "STOP", "ESCALATE", etc.
    private String finalActionLabel;     // "Retry Now", etc.
    private String policyReason;

    public String getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(String policyDecision) {
        this.policyDecision = policyDecision;
    }

    public String getPolicyDecisionLabel() {
        return policyDecisionLabel;
    }

    public void setPolicyDecisionLabel(String policyDecisionLabel) {
        this.policyDecisionLabel = policyDecisionLabel;
    }

    public String getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String finalAction) {
        this.finalAction = finalAction;
    }

    public String getFinalActionLabel() {
        return finalActionLabel;
    }

    public void setFinalActionLabel(String finalActionLabel) {
        this.finalActionLabel = finalActionLabel;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
    }
}
