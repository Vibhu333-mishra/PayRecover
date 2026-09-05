package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.BreakdownItemDto;
import com.payrecover.payrecoverai.dto.DailyTrendItemDto;
import com.payrecover.payrecoverai.dto.DashboardMetricsDto;
import com.payrecover.payrecoverai.dto.DashboardSummaryDto;
import com.payrecover.payrecoverai.dto.MethodBreakdownItemDto;
import com.payrecover.payrecoverai.service.DashboardAnalyticsService;
import com.payrecover.payrecoverai.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PaymentService paymentService;
    private final DashboardAnalyticsService analyticsService;

    public DashboardController(PaymentService paymentService,
                               DashboardAnalyticsService analyticsService) {
        this.paymentService = paymentService;
        this.analyticsService = analyticsService;
    }

    // GET http://localhost:8080/api/dashboard
    // 
    @GetMapping
    public DashboardMetricsDto getDashboardMetrics() {
        return paymentService.getDashboardMetrics();
    }

   
    @GetMapping("/summary")
    public DashboardSummaryDto getSummary() {
        return analyticsService.getSummary();
    }

    // GET http://localhost:8080/api/dashboard/failure-breakdown
    @GetMapping("/failure-breakdown")
    public List<BreakdownItemDto> getFailureBreakdown() {
        return analyticsService.getFailureBreakdown();
    }

    // GET http://localhost:8080/api/dashboard/method-breakdown
    @GetMapping("/method-breakdown")
    public List<MethodBreakdownItemDto> getMethodBreakdown() {
        return analyticsService.getMethodBreakdown();
    }

    
    @GetMapping("/trend")
    public List<DailyTrendItemDto> getDailyTrend(
            @RequestParam(defaultValue = "14") int days) {
        return analyticsService.getDailyTrend(days);
    }
}

