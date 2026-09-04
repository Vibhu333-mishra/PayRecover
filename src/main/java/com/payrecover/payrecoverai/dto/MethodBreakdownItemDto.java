package com.payrecover.payrecoverai.dto;

/**
 * Per-payment-method health, for the "Which payment method fails most?" chart.
 *
 * Example row:
 *   { "method": "UPI", "total": 24, "failed": 11, "recovered": 5, "failureRate": 45.83 }
 *
 * This is a genuinely useful merchant insight: it tells them whether the
 * revenue leak is concentrated in one rail (say UPI) or spread evenly.
 */
public class MethodBreakdownItemDto {

    private String method;
    private long total;      // all payments made with this method
    private long failed;     // payments that failed at least once
    private long recovered;  // of those, how many we got back
    private double failureRate; // failed / total * 100

    public MethodBreakdownItemDto() {
    }

    public MethodBreakdownItemDto(String method, long total, long failed,
                                  long recovered, double failureRate) {
        this.method = method;
        this.total = total;
        this.failed = failed;
        this.recovered = recovered;
        this.failureRate = failureRate;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getRecovered() {
        return recovered;
    }

    public void setRecovered(long recovered) {
        this.recovered = recovered;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }
}
