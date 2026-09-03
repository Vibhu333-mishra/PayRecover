package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response shape for the Policy Engine evaluation endpoint.
 * Combines payment information, AI recommendation, policy decision verdict,
 * final action, human-readable policy reason, and transparent checks breakdown.
 */
public class PolicyDecisionResponseDto {

    private String paymentId;
    private BigDecimal amount;
    private Integer attempts;
    private String failureCode;
    private String failureCategory;
    private String failureCategoryLabel;

    private String recommendedAction;
    private String recommendedActionLabel;
    private Double confidence;
    private Integer confidencePercent;

    private String policyDecision;       // "ALLOWED", "BLOCKED", "ESCALATED"
    private String policyDecisionLabel;  // "Recovery Allowed", etc.
    private String finalAction;          // "RETRY", "STOP", "ESCALATE", etc.
    private String finalActionLabel;     // "Retry Now", etc.
    private String policyReason;

    private List<PolicyCheckResultDto> checks = new ArrayList<>();
    private LocalDateTime evaluatedAt;

    public PolicyDecisionResponseDto() {
    }

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

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getConfidencePercent() {
        return confidencePercent;
    }

    public void setConfidencePercent(Integer confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

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

    public List<PolicyCheckResultDto> getChecks() {
        return checks;
    }

    public void setChecks(List<PolicyCheckResultDto> checks) {
        this.checks = checks;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
