package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.AiDiagnosisResponseDto;
import com.payrecover.payrecoverai.dto.PaymentResponseDto;
import com.payrecover.payrecoverai.service.AiDiagnosisService;
import com.payrecover.payrecoverai.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AiDiagnosisService aiDiagnosisService;

    public PaymentController(PaymentService paymentService,
                             AiDiagnosisService aiDiagnosisService) {
        this.paymentService = paymentService;
        this.aiDiagnosisService = aiDiagnosisService;
    }

    // GET http://localhost:8080/api/payments
    @GetMapping
    public List<PaymentResponseDto> getAllPayments() {
        return paymentService.getAllPayments();
    }

    // GET http://localhost:8080/api/payments/failed
    @GetMapping("/failed")
    public List<PaymentResponseDto> getFailedPayments() {
        return paymentService.getFailedPayments();
    }

    
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentByPaymentId(paymentId);
    }

    
    @PostMapping("/{paymentId}/analyze")
    public AiDiagnosisResponseDto analyzePayment(@PathVariable String paymentId) {
        return aiDiagnosisService.analyze(paymentId);
    }

    
    @GetMapping("/{paymentId}/diagnosis")
    public ResponseEntity<AiDiagnosisResponseDto> getLatestDiagnosis(@PathVariable String paymentId) {
        return aiDiagnosisService.getLatestDiagnosis(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
