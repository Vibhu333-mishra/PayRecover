package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for recovery simulation output.
 * Gives a full visual timeline of the recovery pipeline:
 * FAILED PAYMENT -> AI DIAGNOSIS -> POLICY CHECK -> SIMULATED EXECUTION -> FINAL RESULT
 */
public class RecoverySimulationResponseDto {

    private String paymentId;
    private BigDecimal amount;
    private Integer attemptNumber;
    private String initialStatus;
    private String finalStatus;

    private String failureCategory;
    private String failureCategoryLabel;
    private String recommendedAction;
    private String recommendedActionLabel;

    private String policyDecision;
    private String policyDecisionLabel;
    private String finalAction;
    private String finalActionLabel;
    private String policyReason;

    private String simulatedOutcome;      // "RECOVERED", "FAILED_AGAIN", "NOT_ATTEMPTED", "ESCALATED"
    private BigDecimal amountRecovered;
    private String outcomeDescription;

    private List<String> timeline = new ArrayList<>();
    private LocalDateTime executedAt;

    public RecoverySimulationResponseDto() {
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

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getInitialStatus() {
        return initialStatus;
    }

    public void setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
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

    public String getSimulatedOutcome() {
        return simulatedOutcome;
    }

    public void setSimulatedOutcome(String simulatedOutcome) {
        this.simulatedOutcome = simulatedOutcome;
    }

    public BigDecimal getAmountRecovered() {
        return amountRecovered;
    }

    public void setAmountRecovered(BigDecimal amountRecovered) {
        this.amountRecovered = amountRecovered;
    }

    public String getOutcomeDescription() {
        return outcomeDescription;
    }

    public void setOutcomeDescription(String outcomeDescription) {
        this.outcomeDescription = outcomeDescription;
    }

    public List<String> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<String> timeline) {
        this.timeline = timeline;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
