package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.DashboardMetricsDto;
import com.payrecover.payrecoverai.dto.PaymentResponseDto;
import com.payrecover.payrecoverai.entity.AiDiagnosis;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.entity.RecoveryActionEntity;
import com.payrecover.payrecoverai.exception.ResourceNotFoundException;
import com.payrecover.payrecoverai.repository.AiDiagnosisRepository;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import com.payrecover.payrecoverai.repository.RecoveryActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All business logic for payments lives here, NOT in the controller.
 * The controller's only job is to translate HTTP <-> Java method calls.
 * This separation is what lets us unit-test business logic without spinning
 * up a web server, and lets us reuse this logic from multiple controllers
 * later if needed.
 *
 * PHASE 5 & 6 ADDITION
 * The Failed Payments table carries the latest AI diagnosis and latest Policy Engine
 * verdict for every payment row (if available).
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final RecoveryActionRepository recoveryActionRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          AiDiagnosisRepository aiDiagnosisRepository,
                          RecoveryActionRepository recoveryActionRepository) {
        this.paymentRepository = paymentRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
        this.recoveryActionRepository = recoveryActionRepository;
    }

    public List<PaymentResponseDto> getAllPayments() {
        Map<String, AiDiagnosis> latestDiag = latestDiagnosisByPaymentId();
        Map<String, RecoveryActionEntity> latestAction = latestRecoveryActionByPaymentId();

        return paymentRepository.findAll()
                .stream()
                .map(p -> toDto(p, latestDiag.get(p.getPaymentId()), latestAction.get(p.getPaymentId())))
                .collect(Collectors.toList());
    }

    public List<PaymentResponseDto> getFailedPayments() {
        List<PaymentStatus> failedLikeStatuses = Arrays.asList(
                PaymentStatus.FAILED, PaymentStatus.FAILED_AGAIN
        );

        Map<String, AiDiagnosis> latestDiag = latestDiagnosisByPaymentId();
        Map<String, RecoveryActionEntity> latestAction = latestRecoveryActionByPaymentId();

        return paymentRepository.findByStatusIn(failedLikeStatuses)
                .stream()
                .map(p -> toDto(p, latestDiag.get(p.getPaymentId()), latestAction.get(p.getPaymentId())))
                .collect(Collectors.toList());
    }

    public PaymentResponseDto getPaymentByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        AiDiagnosis latestDiag = aiDiagnosisRepository
                .findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .orElse(null);

        RecoveryActionEntity latestAction = recoveryActionRepository
                .findByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .findFirst()
                .orElse(null);

        return toDto(payment, latestDiag, latestAction);
    }

    /**
     * Computes every dashboard number FROM THE DATABASE, live, on every call.
     * Nothing here is hardcoded -- that's a hard requirement from the spec
     * (Section 9: "Do NOT hardcode these numbers").
     */
    public DashboardMetricsDto getDashboardMetrics() {
        long total = paymentRepository.count();
        long successOnFirstTry = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long recovered = paymentRepository.countByStatus(PaymentStatus.RECOVERED);
        long failedAgain = paymentRepository.countByStatus(PaymentStatus.FAILED_AGAIN);
        long failedNotYetRetried = paymentRepository.countByStatus(PaymentStatus.FAILED);

        // Every payment that failed AT LEAST ONCE, regardless of what happened next.
        long everFailed = failedNotYetRetried + failedAgain + recovered;

        // Payments that ended up in a successful state overall.
        long successfulPayments = successOnFirstTry + recovered;

        // A "recovery attempt" = we actually tried a retry (RECOVERED or FAILED_AGAIN).
        // Plain FAILED payments have not been retried yet, so they don't count here.
        long recoveryAttempts = recovered + failedAgain;

        // Recovery Rate = Recovered / Eligible Failed Payments x 100
        // "Eligible failed payments" = the ones we actually attempted to recover.
        double recoveryRate = recoveryAttempts == 0
                ? 0.0
                : (recovered * 100.0) / recoveryAttempts;
        recoveryRate = round2(recoveryRate);

        BigDecimal revenueRecovered = paymentRepository.findByStatus(PaymentStatus.RECOVERED)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardMetricsDto dto = new DashboardMetricsDto();
        dto.setTotalPayments(total);
        dto.setSuccessfulPayments(successfulPayments);
        dto.setFailedPayments(everFailed);
        dto.setRecoveryAttempts(recoveryAttempts);
        dto.setRecoveredPayments(recovered);
        dto.setRevenueRecovered(revenueRecovered);
        dto.setRecoveryRate(recoveryRate);
        return dto;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Builds "paymentId -> its most recent diagnosis" in ONE database query.
     *
     * WHY NOT JUST QUERY INSIDE THE LOOP?
     * That is the classic "N+1 query" bug: 80 payments would mean 1 query for
     * the payments plus 80 more for the diagnoses. Here we pull every diagnosis
     * once, already sorted newest-first, and keep the first one we meet for each
     * payment id -- which, because of that sort order, is the latest one.
     *
     * putIfAbsent() is what makes that work: it only stores a value if the key
     * is not already present, so later (older) rows are ignored.
     *
     * The query is findAllWithPaymentNewestFirst(), which uses "join fetch" so
     * d.getPayment() below is already loaded and cannot throw
     * LazyInitializationException.
     */
    private Map<String, AiDiagnosis> latestDiagnosisByPaymentId() {
        Map<String, AiDiagnosis> latestByPaymentId = new HashMap<>();
        for (AiDiagnosis d : aiDiagnosisRepository.findAllWithPaymentNewestFirst()) {
            latestByPaymentId.putIfAbsent(d.getPayment().getPaymentId(), d);
        }
        return latestByPaymentId;
    }

    private Map<String, RecoveryActionEntity> latestRecoveryActionByPaymentId() {
        Map<String, RecoveryActionEntity> latestByPaymentId = new HashMap<>();
        for (RecoveryActionEntity r : recoveryActionRepository.findAllByOrderByCreatedAtDesc()) {
            if (r.getPayment() != null) {
                latestByPaymentId.putIfAbsent(r.getPayment().getPaymentId(), r);
            }
        }
        return latestByPaymentId;
    }

    private PaymentResponseDto toDto(Payment payment, AiDiagnosis diagnosis, RecoveryActionEntity actionEntity) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setCustomerId(payment.getCustomerId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setProvider(payment.getProvider());
        dto.setFailureCode(payment.getFailureCode());
        dto.setAttempts(payment.getAttempts());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());

        if (diagnosis == null) {
            dto.setAnalyzed(false);
        } else {
            dto.setAnalyzed(true);
            if (diagnosis.getFailureCategory() != null) {
                dto.setFailureCategory(diagnosis.getFailureCategory().name());
                dto.setFailureCategoryLabel(diagnosis.getFailureCategory().getDisplayName());
            }
            if (diagnosis.getRecommendedAction() != null) {
                dto.setAiRecommendation(diagnosis.getRecommendedAction().name());
                dto.setAiRecommendationLabel(diagnosis.getRecommendedAction().getDisplayName());
            }
            if (diagnosis.getConfidence() != null) {
                dto.setConfidencePercent((int) Math.round(diagnosis.getConfidence() * 100));
            }
            if (diagnosis.getAiSource() != null) {
                dto.setAiSource(diagnosis.getAiSource().name());
            }
        }

        if (actionEntity != null) {
            if (actionEntity.getPolicyDecision() != null) {
                dto.setPolicyDecision(actionEntity.getPolicyDecision().name());
                dto.setPolicyDecisionLabel(actionEntity.getPolicyDecision().getDisplayName());
            }
            if (actionEntity.getFinalAction() != null) {
                dto.setFinalAction(actionEntity.getFinalAction().name());
                dto.setFinalActionLabel(actionEntity.getFinalAction().getDisplayName());
            }
            dto.setPolicyReason(actionEntity.getPolicyReason());
            dto.setRecoveryOutcome(actionEntity.getOutcome());
            dto.setAmountRecovered(actionEntity.getAmountRecovered());
        }

        return dto;
    }
}
