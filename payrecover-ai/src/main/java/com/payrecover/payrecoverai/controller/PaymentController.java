package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.PaymentResponseDto;
import com.payrecover.payrecoverai.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
}
