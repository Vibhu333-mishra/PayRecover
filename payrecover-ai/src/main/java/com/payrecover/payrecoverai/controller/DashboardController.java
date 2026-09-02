package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.DashboardMetricsDto;
import com.payrecover.payrecoverai.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PaymentService paymentService;

    public DashboardController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // GET http://localhost:8080/api/dashboard
    @GetMapping
    public DashboardMetricsDto getDashboardMetrics() {
        return paymentService.getDashboardMetrics();
    }
}

