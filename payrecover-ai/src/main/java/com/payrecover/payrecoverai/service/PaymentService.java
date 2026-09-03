package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.DashboardMetricsDto;
import com.payrecover.payrecoverai.dto.PaymentResponseDto;
import com.payrecover.payrecoverai.entity.AiDiagnosis;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.exception.ResourceNotFoundException;
import com.payrecover.payrecoverai.repository.AiDiagnosisRepository;
import com.payrecover.payrecoverai.repository.PaymentRepository;
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
 * PHASE 5 ADDITION
 * The Failed Payments table in the spec has an "AI Recommendation" column, so
 * every payment row now carries the *latest* AI diagnosis for that payment (if
 * one exists). The diagnosis itself is produced elsewhere (AiDiagnosisService);
 * this class only reads it and copies it onto the DTO.
 *
 * @Transactional(readOnly = true) at class level: every method here only reads.
 * Telling the database that up front lets it skip write bookkeeping, and it
 * keeps one database session open for the whole method, which is what makes
 * lazy-loaded relationships safe to touch.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;

    // Constructor injection: Spring sees this is the only constructor and
    // automatically supplies the beans it needs. Preferred over @Autowired on a
    // field because it makes dependencies explicit and makes the class easy to
    // unit test (just call `new PaymentService(mockRepo, mockDiagnosisRepo)`).
    public PaymentService(PaymentRepository paymentRepository,
                          AiDiagnosisRepository aiDiagnosisRepository) {
        this.paymentRepository = paymentRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
    }

    public List<PaymentResponseDto> getAllPayments() {
        Map<String, AiDiagnosis> latest = latestDiagnosisByPaymentId();

        return paymentRepository.findAll()
                .stream()
                .map(p -> toDto(p, latest.get(p.getPaymentId())))
                .collect(Collectors.toList());
    }

    public List<PaymentResponseDto> getFailedPayments() {
        // "Failed" from the merchant's point of view = currently not in a good
        // state: either not-yet-retried (FAILED) or retried-and-still-failed
        // (FAILED_AGAIN). RECOVERED payments are excluded because they are
        // now successful.
        List<PaymentStatus> failedLikeStatuses = Arrays.asList(
                PaymentStatus.FAILED, PaymentStatus.FAILED_AGAIN
        );

        Map<String, AiDiagnosis> latest = latestDiagnosisByPaymentId();

        return paymentRepository.findByStatusIn(failedLikeStatuses)
                .stream()
                .map(p -> toDto(p, latest.get(p.getPaymentId())))
                .collect(Collectors.toList());
    }

    public PaymentResponseDto getPaymentByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        // Single row -> single targeted query is cheaper than loading the map.
        AiDiagnosis latest = aiDiagnosisRepository
                .findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .orElse(null);

        return toDto(payment, latest);
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

    // Converts an Entity (database row) into a DTO (API response shape).
    /**
     * Same conversion, plus the AI columns.
     *
     * @param diagnosis the newest diagnosis for this payment, or null if the
     *                  payment has never been analysed. Null is a completely
     *                  normal state -- most rows will be null until the user
     *                  clicks "Analyze" -- so it is handled, not treated as an
     *                  error.
     */
    private PaymentResponseDto toDto(Payment payment, AiDiagnosis diagnosis) {
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
            // The UI reads this single flag and renders "Not analyzed yet"
            // instead of null-checking five separate fields.
            dto.setAnalyzed(false);
            return dto;
        }

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
            // Stored 0.0-1.0, shown to humans as 0-100.
            dto.setConfidencePercent((int) Math.round(diagnosis.getConfidence() * 100));
        }
        if (diagnosis.getAiSource() != null) {
            // Lets the UI show the "AI unavailable - fallback rules used" badge.
            dto.setAiSource(diagnosis.getAiSource().name());
        }
        return dto;
    }
}
