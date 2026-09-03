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

/**
 * @RestController = @Controller + @ResponseBody. Every method's return value
 * is automatically converted to JSON (Spring uses the Jackson library under
 * the hood) and written to the HTTP response body.
 *
 * @RequestMapping("/api/payments") sets the common URL prefix for every
 * method in this class.
 */
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

    // GET http://localhost:8080/api/payments/PAY1001
    // @PathVariable pulls the {paymentId} segment out of the URL and passes
    // it in as a method argument.
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentByPaymentId(paymentId);
    }

    /**
     * POST http://localhost:8080/api/payments/PAY1001/analyze
     *
     * This is the "Analyze" button in the UI. It runs a FRESH diagnosis every
     * time it is called.
     *
     * WHY POST AND NOT GET
     * GET is supposed to be read-only and safe to repeat or cache. This call
     * costs an external API request and writes two new database rows, so it is
     * not read-only. POST is the honest verb.
     *
     * It has no request body -- everything needed is already in the URL and the
     * database, so there is nothing for the caller to send.
     */
    @PostMapping("/{paymentId}/analyze")
    public AiDiagnosisResponseDto analyzePayment(@PathVariable String paymentId) {
        return aiDiagnosisService.analyze(paymentId);
    }

    /**
     * GET http://localhost:8080/api/payments/PAY1001/diagnosis
     *
     * Reads back the most recent stored diagnosis WITHOUT calling the LLM.
     * Use this when re-opening a payment the user already analysed -- free and
     * instant.
     *
     * ResponseEntity lets us control the HTTP status code by hand. If this
     * payment has never been analysed we return 204 No Content instead of an
     * error, because "not analysed yet" is a normal state, not a failure. In
     * React: if (res.status === 204) show the Analyze button.
     */
    @GetMapping("/{paymentId}/diagnosis")
    public ResponseEntity<AiDiagnosisResponseDto> getLatestDiagnosis(@PathVariable String paymentId) {
        return aiDiagnosisService.getLatestDiagnosis(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
