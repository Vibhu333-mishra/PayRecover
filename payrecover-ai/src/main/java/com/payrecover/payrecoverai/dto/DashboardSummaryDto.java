package com.payrecover.payrecoverai.dto;

import java.util.List;

/**
 * ONE response that contains everything the dashboard screen needs.
 *
 * Why bundle it? Without this, the React dashboard would fire four separate
 * HTTP requests on page load (metrics, failure breakdown, method breakdown,
 * trend) and you would have to juggle four loading states. One endpoint =
 * one fetch, one loading spinner, one error path. Much less React code.
 *
 * The individual endpoints still exist for anyone who only needs one chart.
 */
public class DashboardSummaryDto {

    private DashboardMetricsDto metrics;
    private List<BreakdownItemDto> failureBreakdown;
    private List<MethodBreakdownItemDto> methodBreakdown;
    private List<DailyTrendItemDto> dailyTrend;

    public DashboardSummaryDto() {
    }

    public DashboardMetricsDto getMetrics() {
        return metrics;
    }

    public void setMetrics(DashboardMetricsDto metrics) {
        this.metrics = metrics;
    }

    public List<BreakdownItemDto> getFailureBreakdown() {
        return failureBreakdown;
    }

    public void setFailureBreakdown(List<BreakdownItemDto> failureBreakdown) {
        this.failureBreakdown = failureBreakdown;
    }

    public List<MethodBreakdownItemDto> getMethodBreakdown() {
        return methodBreakdown;
    }

    public void setMethodBreakdown(List<MethodBreakdownItemDto> methodBreakdown) {
        this.methodBreakdown = methodBreakdown;
    }

    public List<DailyTrendItemDto> getDailyTrend() {
        return dailyTrend;
    }

    public void setDailyTrend(List<DailyTrendItemDto> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }
}
