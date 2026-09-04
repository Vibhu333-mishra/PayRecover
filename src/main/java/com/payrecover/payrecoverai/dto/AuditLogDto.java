package com.payrecover.payrecoverai.dto;

import java.time.LocalDateTime;

/**
 * One line of the Audit Logs screen.
 *
 * Flat and string-heavy on purpose: an audit view is read, sorted and filtered,
 * never edited, so simple text columns are exactly right. Nulls are expected --
 * a diagnosis row has no policy decision yet, and a policy row has no AI
 * confidence of its own.
 */
public class AuditLogDto {

    private Long id;
    private LocalDateTime timestamp;
    private String paymentId;
    private String eventType;
    private String failureCategory;
    private String aiRecommendation;
    private Integer confidencePercent;
    private String aiSource;
    private String policyDecision;
    private String finalAction;
    private String result;
    private String details;

    public AuditLogDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
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

    public String getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(String policyDecision) {
        this.policyDecision = policyDecision;
    }

    public String getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String finalAction) {
        this.finalAction = finalAction;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
