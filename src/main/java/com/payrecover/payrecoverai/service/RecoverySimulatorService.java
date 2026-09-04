package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.PolicyDecisionResponseDto;
import com.payrecover.payrecoverai.dto.RecoveryActionDto;
import com.payrecover.payrecoverai.dto.RecoverySimulationResponseDto;
import com.payrecover.payrecoverai.entity.*;
import com.payrecover.payrecoverai.exception.ResourceNotFoundException;
import com.payrecover.payrecoverai.repository.AiDiagnosisRepository;
import com.payrecover.payrecoverai.repository.AuditLogRepository;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import com.payrecover.payrecoverai.repository.RecoveryActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Phase 7 Recovery Simulation.
 *
 * SIMULATION LOGIC:
 * 1. Executes or retrieves Policy Decision (Phase 6).
 * 2. If BLOCKED -> outcome = NOT_ATTEMPTED, payment status and attempt count remain unchanged.
 * 3. If ESCALATED -> outcome = ESCALATED, payment status and attempt count remain unchanged.
 * 4. If ALLOWED and finalAction is RETRY / WAIT_AND_RETRY:
 *    - Increments payment.attempts (attempts + 1).
 *    - Simulates retry outcome with 85% success probability (RECOVERED) and 15% failure (FAILED_AGAIN).
 *    - On RECOVERED: payment.status = RECOVERED, amountRecovered = payment.amount.
 *    - On FAILED_AGAIN: payment.status = FAILED_AGAIN, amountRecovered = 0.
 * 5. Updates recovery_actions entity and writes eventType = "RECOVERY_ATTEMPT" to audit_logs.
 * 6. Returns visual decision timeline in DTO.
 */
@Service
public class RecoverySimulatorService {

    private static final Logger log = LoggerFactory.getLogger(RecoverySimulatorService.class);
    private static final String EVENT_RECOVERY_ATTEMPT = "RECOVERY_ATTEMPT";

    private final PaymentRepository paymentRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PolicyEvaluationService policyEvaluationService;

    public RecoverySimulatorService(PaymentRepository paymentRepository,
                                   AiDiagnosisRepository aiDiagnosisRepository,
                                   RecoveryActionRepository recoveryActionRepository,
                                   AuditLogRepository auditLogRepository,
                                   PolicyEvaluationService policyEvaluationService) {
        this.paymentRepository = paymentRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
        this.recoveryActionRepository = recoveryActionRepository;
        this.auditLogRepository = auditLogRepository;
        this.policyEvaluationService = policyEvaluationService;
    }

    @Transactional
    public RecoverySimulationResponseDto simulateRecovery(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Payment " + paymentId + " succeeded on initial transaction. No recovery needed.");
        }

        String initialStatus = payment.getStatus().name();

        // 1. Evaluate policy (or fetch existing policy evaluation)
        PolicyDecisionResponseDto policyDto = policyEvaluationService.evaluatePolicy(paymentId);

        // Fetch the corresponding entities for update and audit logging
        AiDiagnosis diagnosis = aiDiagnosisRepository
                .findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .orElse(null);

        RecoveryActionEntity recoveryEntity = recoveryActionRepository
                .findByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recovery action entity missing after policy evaluation for " + paymentId));

        PolicyDecision decision = PolicyDecision.valueOf(policyDto.getPolicyDecision());
        RecoveryActionType finalAction = RecoveryActionType.fromCode(policyDto.getFinalAction());

        String outcome;
        BigDecimal amountRecovered = BigDecimal.ZERO;
        String outcomeDescription;

        // 2. Perform execution simulation based on Policy Decision
        if (decision == PolicyDecision.BLOCKED) {
            outcome = "NOT_ATTEMPTED";
            outcomeDescription = "Recovery BLOCKED by policy rules. No attempt sent to gateway.";
        } else if (decision == PolicyDecision.ESCALATED) {
            outcome = "ESCALATED";
            outcomeDescription = "Payment ESCALATED to merchant support queue for human decision.";
        } else {
            // Decision is ALLOWED
            if (finalAction.isRetryAction()) {
                // Increment attempt count ONLY for retry actions
                int currentAttempts = payment.getAttempts() == null ? 1 : payment.getAttempts();
                payment.setAttempts(currentAttempts + 1);

                // Deterministic 85% success simulation based on payment ID hash
                boolean success = (Math.abs(paymentId.hashCode()) % 100) < 85;

                if (success) {
                    outcome = "RECOVERED";
                    amountRecovered = payment.getAmount();
                    payment.setStatus(PaymentStatus.RECOVERED);
                    outcomeDescription = "Simulated bank retry succeeded! Revenue of ₹" + payment.getAmount() + " recovered.";
                } else {
                    outcome = "FAILED_AGAIN";
                    amountRecovered = BigDecimal.ZERO;
                    payment.setStatus(PaymentStatus.FAILED_AGAIN);
                    outcomeDescription = "Simulated bank retry failed on gateway after attempt " + payment.getAttempts() + ".";
                }
            } else if (finalAction == RecoveryActionType.ALTERNATE_PAYMENT_METHOD) {
                outcome = "ACTION_PROMPTED";
                outcomeDescription = "Customer prompted to pay using an alternate payment method.";
            } else if (finalAction == RecoveryActionType.STOP) {
                outcome = "NOT_ATTEMPTED";
                outcomeDescription = "Action set to STOP. No further retry attempted.";
            } else {
                outcome = "ESCALATED";
                outcomeDescription = "Escalated to human operator.";
            }
        }

        // 3. Save updated payment entity
        paymentRepository.save(payment);

        // 4. Update recovery action entity
        recoveryEntity.setOutcome(outcome);
        recoveryEntity.setAmountRecovered(amountRecovered);
        recoveryActionRepository.save(recoveryEntity);

        // 5. Write RECOVERY_ATTEMPT audit log entry
        writeAuditLog(payment, diagnosis, policyDto, outcome, outcomeDescription);

        log.info("[RecoverySimulator] {} -> Policy: {}, Action: {}, Outcome: {}, Final Status: {}",
                paymentId, decision, finalAction, outcome, payment.getStatus());

        return buildResponseDto(payment, initialStatus, policyDto, outcome, amountRecovered, outcomeDescription);
    }

    @Transactional(readOnly = true)
    public List<RecoveryActionDto> getRecoveryHistoryForPayment(String paymentId) {
        paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        return recoveryActionRepository.findByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .map(this::toActionDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecoveryActionDto> getAllRecoveries() {
        return recoveryActionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toActionDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private RecoveryActionDto toActionDto(RecoveryActionEntity entity) {
        RecoveryActionDto dto = new RecoveryActionDto();
        dto.setId(entity.getId());
        if (entity.getPayment() != null) {
            dto.setPaymentId(entity.getPayment().getPaymentId());
        }
        if (entity.getRecommendedAction() != null) {
            dto.setRecommendedAction(entity.getRecommendedAction().name());
            dto.setRecommendedActionLabel(entity.getRecommendedAction().getDisplayName());
        }
        if (entity.getPolicyDecision() != null) {
            dto.setPolicyDecision(entity.getPolicyDecision().name());
            dto.setPolicyDecisionLabel(entity.getPolicyDecision().getDisplayName());
        }
        dto.setPolicyReason(entity.getPolicyReason());
        if (entity.getFinalAction() != null) {
            dto.setFinalAction(entity.getFinalAction().name());
            dto.setFinalActionLabel(entity.getFinalAction().getDisplayName());
        }
        dto.setAttemptNumber(entity.getAttemptNumber());
        dto.setOutcome(entity.getOutcome());
        dto.setAmountRecovered(entity.getAmountRecovered());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private void writeAuditLog(Payment payment,
                               AiDiagnosis diagnosis,
                               PolicyDecisionResponseDto policyDto,
                               String outcome,
                               String outcomeDescription) {
        AuditLog logEntry = new AuditLog();
        logEntry.setPaymentId(payment.getPaymentId());
        logEntry.setEventType(EVENT_RECOVERY_ATTEMPT);
        if (diagnosis != null) {
            logEntry.setFailureCategory(diagnosis.getFailureCategory());
            logEntry.setAiRecommendation(diagnosis.getRecommendedAction());
            logEntry.setConfidence(diagnosis.getConfidence());
            logEntry.setAiSource(diagnosis.getAiSource());
        }
        logEntry.setPolicyDecision(PolicyDecision.valueOf(policyDto.getPolicyDecision()));
        logEntry.setFinalAction(RecoveryActionType.fromCode(policyDto.getFinalAction()));
        logEntry.setResult(outcome);
        logEntry.setDetails(outcomeDescription);

        auditLogRepository.save(logEntry);
    }

    private RecoverySimulationResponseDto buildResponseDto(Payment payment,
                                                           String initialStatus,
                                                           PolicyDecisionResponseDto policyDto,
                                                           String outcome,
                                                           BigDecimal amountRecovered,
                                                           String outcomeDescription) {
        RecoverySimulationResponseDto dto = new RecoverySimulationResponseDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setAttemptNumber(payment.getAttempts());
        dto.setInitialStatus(initialStatus);
        dto.setFinalStatus(payment.getStatus().name());

        dto.setFailureCategory(policyDto.getFailureCategory());
        dto.setFailureCategoryLabel(policyDto.getFailureCategoryLabel());
        dto.setRecommendedAction(policyDto.getRecommendedAction());
        dto.setRecommendedActionLabel(policyDto.getRecommendedActionLabel());

        dto.setPolicyDecision(policyDto.getPolicyDecision());
        dto.setPolicyDecisionLabel(policyDto.getPolicyDecisionLabel());
        dto.setFinalAction(policyDto.getFinalAction());
        dto.setFinalActionLabel(policyDto.getFinalActionLabel());
        dto.setPolicyReason(policyDto.getPolicyReason());

        dto.setSimulatedOutcome(outcome);
        dto.setAmountRecovered(amountRecovered);
        dto.setOutcomeDescription(outcomeDescription);
        dto.setExecutedAt(LocalDateTime.now());

        // Build 5-step transparent visual timeline list
        List<String> timeline = new ArrayList<>();
        timeline.add("Step 1: FAILED PAYMENT [Status: " + initialStatus + ", Amount: ₹" + payment.getAmount() + ", Code: " + payment.getFailureCode() + "]");
        timeline.add("Step 2: AI DIAGNOSIS [Category: " + policyDto.getFailureCategoryLabel() + ", AI Recommendation: " + policyDto.getRecommendedActionLabel() + "]");
        timeline.add("Step 3: POLICY ENGINE CHECK [Decision: " + policyDto.getPolicyDecisionLabel() + ", Final Action: " + policyDto.getFinalActionLabel() + "]");
        timeline.add("Step 4: SIMULATED EXECUTION [Attempt: " + payment.getAttempts() + ", Action: " + policyDto.getFinalActionLabel() + "]");
        timeline.add("Step 5: FINAL RESULT [Outcome: " + outcome + ", New Status: " + payment.getStatus().name() + ", Revenue Recovered: ₹" + amountRecovered + "]");

        dto.setTimeline(timeline);
        return dto;
    }
}
