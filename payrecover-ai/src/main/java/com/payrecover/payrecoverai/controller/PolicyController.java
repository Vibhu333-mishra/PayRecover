package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.PolicyDecisionResponseDto;
import com.payrecover.payrecoverai.service.PolicyEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Policy Engine evaluation endpoints.
 *
 * POST /api/payments/{paymentId}/policy   - Run Policy Engine on payment's AI diagnosis & save result
 * GET  /api/payments/{paymentId}/policy   - Retrieve latest policy verdict for payment
 */
@RestController
@RequestMapping("/api/payments")
public class PolicyController {

    private final PolicyEvaluationService policyEvaluationService;

    public PolicyController(PolicyEvaluationService policyEvaluationService) {
        this.policyEvaluationService = policyEvaluationService;
    }

    /**
     * POST http://localhost:8080/api/payments/PAY1001/policy
     * Runs the Policy Engine rules against the latest AI diagnosis for the payment.
     */
    @PostMapping("/{paymentId}/policy")
    public PolicyDecisionResponseDto evaluatePolicy(@PathVariable String paymentId) {
        return policyEvaluationService.evaluatePolicy(paymentId);
    }

    /**
     * GET http://localhost:8080/api/payments/PAY1001/policy
     * Returns the most recent policy verdict without re-evaluating. Returns 204 if not evaluated yet.
     */
    @GetMapping("/{paymentId}/policy")
    public ResponseEntity<PolicyDecisionResponseDto> getLatestPolicyDecision(@PathVariable String paymentId) {
        return policyEvaluationService.getLatestPolicyDecision(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
