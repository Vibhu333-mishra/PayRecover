package com.payrecover.payrecoverai.dto;

/**
 * Represents an individual policy check item for explainability in the UI / API.
 * Example:
 *   ruleName: "Attempt Limit Check"
 *   passed: true / false
 *   detail: "Attempts (1) < Max Allowed (2)"
 */
public class PolicyCheckResultDto {

    private String ruleName;
    private boolean passed;
    private String detail;

    public PolicyCheckResultDto() {
    }

    public PolicyCheckResultDto(String ruleName, boolean passed, String detail) {
        this.ruleName = ruleName;
        this.passed = passed;
        this.detail = detail;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
