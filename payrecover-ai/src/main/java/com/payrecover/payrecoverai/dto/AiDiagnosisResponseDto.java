package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What POST /api/payments/{paymentId}/analyze returns -- everything the
 * "AI Payment Analysis" panel needs, in one object.
 *
 * DESIGN NOTES
 *  - Enums are sent as BOTH the raw name and a display label
 *    (failureCategory = "BANK_TIMEOUT", failureCategoryLabel = "Bank Timeout").
 *    The raw name is for React logic and badge colours; the label is for humans.
 *    Sending both means the frontend never hardcodes a translation table.
 *  - confidencePercent saves the frontend from float maths: 0.91 -> 91.
 *  - aiAvailable + notice are the honesty fields. When the LLM could not be
 *    reached, aiAvailable is false and notice explains it in plain words.
 */
public class AiDiagnosisResponseDto {

    // --- context, so the panel is self-contained ---
    private String paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String provider;
    private String failureCode;
    private Integer attempts;

    // --- the diagnosis ---
    private String failureCategory;
    private String failureCategoryLabel;
    private String probableReason;
    private String recommendedAction;
    private String recommendedActionLabel;
    private String recommendedActionDescription;
    private double confidence;      // 0.0 - 1.0
    private int confidencePercent;  // 0 - 100
    private String explanation;

    // --- provenance: who produced this, how fast ---
    private String aiSource;
    private boolean aiAvailable;
    private String notice;     // null when the real model answered
    private String modelName;
    private Long latencyMs;
    private LocalDateTime analyzedAt;

    /** Always true. Reminds the UI (and the judges) that no real money moves. */
    private boolean simulation = true;

    public AiDiagnosisResponseDto() {
    }

    // ===== Getters and Setters =====

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getProbableReason() {
        return probableReason;
    }

    public void setProbableReason(String probableReason) {
        this.probableReason = probableReason;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getRecommendedActionLabel() {
        return recommendedActionLabel;
    }

    public void setRecommendedActionLabel(String recommendedActionLabel) {
        this.recommendedActionLabel = recommendedActionLabel;
    }

    public String getRecommendedActionDescription() {
        return recommendedActionDescription;
    }

    public void setRecommendedActionDescription(String recommendedActionDescription) {
        this.recommendedActionDescription = recommendedActionDescription;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getConfidencePercent() {
        return confidencePercent;
    }

    public void setConfidencePercent(int confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getAiSource() {
        return aiSource;
    }

    public void setAiSource(String aiSource) {
        this.aiSource = aiSource;
    }

    public boolean isAiAvailable() {
        return aiAvailable;
    }

    public void setAiAvailable(boolean aiAvailable) {
        this.aiAvailable = aiAvailable;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }
}
