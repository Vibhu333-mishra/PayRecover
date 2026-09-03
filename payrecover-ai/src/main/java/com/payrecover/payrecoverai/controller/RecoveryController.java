package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.RecoveryActionDto;
import com.payrecover.payrecoverai.dto.RecoverySimulationResponseDto;
import com.payrecover.payrecoverai.service.RecoverySimulatorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Phase 7 Recovery Simulator endpoints.
 *
 * POST /api/payments/{paymentId}/recover           - Runs simulated recovery for a payment
 * GET  /api/payments/{paymentId}/recovery-history  - History of recovery attempts for a payment
 * GET  /api/recoveries                             - Full recovery actions table across payments
 */
@RestController
@RequestMapping
public class RecoveryController {

    private final RecoverySimulatorService recoverySimulatorService;

    public RecoveryController(RecoverySimulatorService recoverySimulatorService) {
        this.recoverySimulatorService = recoverySimulatorService;
    }

    /**
     * POST http://localhost:8080/api/payments/PAY1001/recover
     * Runs simulated recovery execution for payment PAY1001.
     */
    @PostMapping("/api/payments/{paymentId}/recover")
    public RecoverySimulationResponseDto simulateRecovery(@PathVariable String paymentId) {
        return recoverySimulatorService.simulateRecovery(paymentId);
    }

    /**
     * GET http://localhost:8080/api/payments/PAY1001/recovery-history
     * Returns history of recovery attempts for one payment.
     */
    @GetMapping("/api/payments/{paymentId}/recovery-history")
    public List<RecoveryActionDto> getRecoveryHistoryForPayment(@PathVariable String paymentId) {
        return recoverySimulatorService.getRecoveryHistoryForPayment(paymentId);
    }

    /**
     * GET http://localhost:8080/api/recoveries
     * Full recovery history table across all payments.
     */
    @GetMapping("/api/recoveries")
    public List<RecoveryActionDto> getAllRecoveries() {
        return recoverySimulatorService.getAllRecoveries();
    }
}
