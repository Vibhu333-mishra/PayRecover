package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecoveryActionDto {

    private Long id;
    private String paymentId;
    private String recommendedAction;
    private String recommendedActionLabel;
    private String policyDecision;
    private String policyDecisionLabel;
    private String policyReason;
    private String finalAction;
    private String finalActionLabel;
    private Integer attemptNumber;
    private String outcome;
    private BigDecimal amountRecovered;
    private LocalDateTime createdAt;

    public RecoveryActionDto() {
    }

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

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
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

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getAmountRecovered() {
        return amountRecovered;
    }

    public void setAmountRecovered(BigDecimal amountRecovered) {
        this.amountRecovered = amountRecovered;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
