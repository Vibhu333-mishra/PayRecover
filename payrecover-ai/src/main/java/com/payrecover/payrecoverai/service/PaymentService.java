package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.DashboardMetricsDto;
import com.payrecover.payrecoverai.dto.PaymentResponseDto;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.exception.ResourceNotFoundException;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All business logic for payments lives here, NOT in the controller.
 * The controller's only job is to translate HTTP <-> Java method calls.
 * This separation is what lets us unit-test business logic without spinning
 * up a web server, and lets us reuse this logic from multiple controllers
 * later if needed.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // Constructor injection: Spring sees this is the only constructor and
    // automatically supplies a PaymentRepository bean. Preferred over
    // @Autowired on a field because it makes dependencies explicit and
    // makes the class easy to unit test (just call `new PaymentService(mockRepo)`).
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toDto)
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

        return paymentRepository.findByStatusIn(failedLikeStatuses)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PaymentResponseDto getPaymentByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        return toDto(payment);
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

    // Converts an Entity (database row) into a DTO (API response shape).
    private PaymentResponseDto toDto(Payment payment) {
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
        return dto;
    }
}
