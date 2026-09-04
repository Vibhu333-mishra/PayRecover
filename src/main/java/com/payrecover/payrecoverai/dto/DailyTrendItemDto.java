package com.payrecover.payrecoverai.dto;

import java.time.LocalDate;

/**
 * One point on the "last N days" line chart.
 *
 * Example:
 *   { "date": "2026-09-01", "total": 4, "failed": 2, "recovered": 1 }
 *
 * Days with no activity are still returned (with zeros) so the line chart has
 * an even x-axis instead of jumping over gaps.
 */
public class DailyTrendItemDto {

    private LocalDate date;
    private long total;
    private long failed;
    private long recovered;

    public DailyTrendItemDto() {
    }

    public DailyTrendItemDto(LocalDate date, long total, long failed, long recovered) {
        this.date = date;
        this.total = total;
        this.failed = failed;
        this.recovered = recovered;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
}
