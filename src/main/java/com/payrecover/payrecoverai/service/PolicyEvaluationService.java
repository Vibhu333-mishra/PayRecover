package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.PolicyDecisionResponseDto;
import com.payrecover.payrecoverai.entity.AiDiagnosis;
import com.payrecover.payrecoverai.entity.AuditLog;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.entity.PolicyDecision;
import com.payrecover.payrecoverai.entity.RecoveryActionEntity;
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
import java.util.Optional;

/**
 * Service orchestrating policy decision evaluation, persisting the decision record into
 * recovery_actions, and writing the POLICY_DECISION audit log entry.
 */
@Service
public class PolicyEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationService.class);
    private static final String EVENT_POLICY_DECISION = "POLICY_DECISION";

    private final PaymentRepository paymentRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiDiagnosisService aiDiagnosisService;
    private final PolicyEngine policyEngine;

    public PolicyEvaluationService(PaymentRepository paymentRepository,
                                   AiDiagnosisRepository aiDiagnosisRepository,
                                   RecoveryActionRepository recoveryActionRepository,
                                   AuditLogRepository auditLogRepository,
                                   AiDiagnosisService aiDiagnosisService,
                                   PolicyEngine policyEngine) {
        this.paymentRepository = paymentRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
        this.recoveryActionRepository = recoveryActionRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiDiagnosisService = aiDiagnosisService;
        this.policyEngine = policyEngine;
    }

    @Transactional
    public PolicyDecisionResponseDto evaluatePolicy(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Payment " + paymentId + " succeeded and requires no policy evaluation.");
        }

        // Get latest diagnosis, or trigger analysis if not analyzed yet
        AiDiagnosis diagnosis = aiDiagnosisRepository
                .findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .orElseGet(() -> {
                    log.info("[PolicyEvaluation] No existing diagnosis found for {}. Triggering AI diagnosis first.", paymentId);
                    aiDiagnosisService.analyze(paymentId);
                    return aiDiagnosisRepository.findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                            .orElseThrow(() -> new IllegalStateException("Failed to retrieve diagnosis after analysis for " + paymentId));
                });

        PolicyEngine.EvaluationResult evalResult = policyEngine.evaluate(payment, diagnosis);

        // Persist recovery action record
        RecoveryActionEntity actionEntity = saveRecoveryAction(payment, diagnosis, evalResult);

        // Write audit log entry
        writeAuditLog(payment, diagnosis, evalResult);

        log.info("[PolicyEvaluation] {} -> AI: {}, Policy: {}, Final: {}",
                paymentId,
                diagnosis.getRecommendedAction(),
                evalResult.getDecision(),
                evalResult.getFinalAction());

        return toDto(payment, diagnosis, evalResult);
    }

    @Transactional(readOnly = true)
    public Optional<PolicyDecisionResponseDto> getLatestPolicyDecision(String paymentId) {
        paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        return recoveryActionRepository
                .findByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .findFirst()
                .map(this::toDtoFromEntity);
    }

    private RecoveryActionEntity saveRecoveryAction(Payment payment,
                                                    AiDiagnosis diagnosis,
                                                    PolicyEngine.EvaluationResult evalResult) {
        RecoveryActionEntity entity = new RecoveryActionEntity();
        entity.setPayment(payment);
        entity.setDiagnosis(diagnosis);
        entity.setRecommendedAction(diagnosis.getRecommendedAction());
        entity.setPolicyDecision(evalResult.getDecision());
        entity.setPolicyReason(evalResult.getPolicyReason());
        entity.setFinalAction(evalResult.getFinalAction());
        entity.setAttemptNumber(payment.getAttempts());

        String outcome = switch (evalResult.getDecision()) {
            case ALLOWED -> "POLICY_EVALUATED";
            case BLOCKED -> "BLOCKED";
            case ESCALATED -> "ESCALATED";
        };
        entity.setOutcome(outcome);
        entity.setAmountRecovered(BigDecimal.ZERO);

        return recoveryActionRepository.save(entity);
    }

    private void writeAuditLog(Payment payment, AiDiagnosis diagnosis, PolicyEngine.EvaluationResult evalResult) {
        AuditLog logEntry = new AuditLog();
        logEntry.setPaymentId(payment.getPaymentId());
        logEntry.setEventType(EVENT_POLICY_DECISION);
        logEntry.setFailureCategory(diagnosis.getFailureCategory());
        logEntry.setAiRecommendation(diagnosis.getRecommendedAction());
        logEntry.setConfidence(diagnosis.getConfidence());
        logEntry.setAiSource(diagnosis.getAiSource());
        logEntry.setPolicyDecision(evalResult.getDecision());
        logEntry.setFinalAction(evalResult.getFinalAction());
        logEntry.setResult(evalResult.getDecision().name());
        logEntry.setDetails(evalResult.getPolicyReason());

        auditLogRepository.save(logEntry);
    }

    private PolicyDecisionResponseDto toDto(Payment payment, AiDiagnosis diagnosis, PolicyEngine.EvaluationResult evalResult) {
        PolicyDecisionResponseDto dto = new PolicyDecisionResponseDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setAttempts(payment.getAttempts());
        dto.setFailureCode(payment.getFailureCode());

        dto.setFailureCategory(diagnosis.getFailureCategory().name());
        dto.setFailureCategoryLabel(diagnosis.getFailureCategory().getDisplayName());

        dto.setRecommendedAction(diagnosis.getRecommendedAction().name());
        dto.setRecommendedActionLabel(diagnosis.getRecommendedAction().getDisplayName());

        double confidence = diagnosis.getConfidence() == null ? 0.0 : diagnosis.getConfidence();
        dto.setConfidence(confidence);
        dto.setConfidencePercent((int) Math.round(confidence * 100));

        dto.setPolicyDecision(evalResult.getDecision().name());
        dto.setPolicyDecisionLabel(evalResult.getDecision().getDisplayName());

        dto.setFinalAction(evalResult.getFinalAction().name());
        dto.setFinalActionLabel(evalResult.getFinalAction().getDisplayName());

        dto.setPolicyReason(evalResult.getPolicyReason());
        dto.setChecks(evalResult.getChecks());
        dto.setEvaluatedAt(java.time.LocalDateTime.now());

        return dto;
    }

    private PolicyDecisionResponseDto toDtoFromEntity(RecoveryActionEntity entity) {
        Payment payment = entity.getPayment();
        AiDiagnosis diagnosis = entity.getDiagnosis();

        PolicyDecisionResponseDto dto = new PolicyDecisionResponseDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setAttempts(payment.getAttempts());
        dto.setFailureCode(payment.getFailureCode());

        if (diagnosis != null) {
            dto.setFailureCategory(diagnosis.getFailureCategory().name());
            dto.setFailureCategoryLabel(diagnosis.getFailureCategory().getDisplayName());
            double confidence = diagnosis.getConfidence() == null ? 0.0 : diagnosis.getConfidence();
            dto.setConfidence(confidence);
            dto.setConfidencePercent((int) Math.round(confidence * 100));
        }

        dto.setRecommendedAction(entity.getRecommendedAction().name());
        dto.setRecommendedActionLabel(entity.getRecommendedAction().getDisplayName());

        dto.setPolicyDecision(entity.getPolicyDecision().name());
        dto.setPolicyDecisionLabel(entity.getPolicyDecision().getDisplayName());

        dto.setFinalAction(entity.getFinalAction().name());
        dto.setFinalActionLabel(entity.getFinalAction().getDisplayName());

        dto.setPolicyReason(entity.getPolicyReason());
        dto.setEvaluatedAt(entity.getCreatedAt());

        return dto;
    }
}
