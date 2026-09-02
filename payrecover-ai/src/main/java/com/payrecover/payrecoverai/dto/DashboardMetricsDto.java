package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;

/**
 * Shape of the JSON returned by GET /api/dashboard.
 * Every number here is CALCULATED from the payments table at request time --
 * nothing is hardcoded (see PaymentService.getDashboardMetrics()).
 */
public class DashboardMetricsDto {

    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long recoveryAttempts;
    private long recoveredPayments;
    private BigDecimal revenueRecovered;
    private double recoveryRate; // percentage, e.g. 62.5

    public DashboardMetricsDto() {
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(long successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(long failedPayments) {
        this.failedPayments = failedPayments;
    }

    public long getRecoveryAttempts() {
        return recoveryAttempts;
    }

    public void setRecoveryAttempts(long recoveryAttempts) {
        this.recoveryAttempts = recoveryAttempts;
    }

    public long getRecoveredPayments() {
        return recoveredPayments;
    }

    public void setRecoveredPayments(long recoveredPayments) {
        this.recoveredPayments = recoveredPayments;
    }

    public BigDecimal getRevenueRecovered() {
        return revenueRecovered;
    }

    public void setRevenueRecovered(BigDecimal revenueRecovered) {
        this.revenueRecovered = revenueRecovered;
    }

    public double getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(double recoveryRate) {
        this.recoveryRate = recoveryRate;
    }
}
