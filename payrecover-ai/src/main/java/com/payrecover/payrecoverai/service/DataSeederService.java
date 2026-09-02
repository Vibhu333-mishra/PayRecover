package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * CommandLineRunner is a Spring Boot interface: any bean implementing it has
 * its run() method executed ONCE, automatically, right after the application
 * has fully started up. We use that to auto-populate the database with
 * realistic-looking synthetic payment data, so the app is demo-ready the
 * moment you run it -- no manual SQL inserts required.
 *
 * It's safe to restart the app repeatedly: we check if data already exists
 * first, so we never duplicate records.
 */
@Component
public class DataSeederService implements CommandLineRunner {

    private final PaymentRepository paymentRepository;
    private final Random random = new Random();

    private static final int TOTAL_RECORDS = 80;

    private static final String[] PAYMENT_METHODS = {"UPI", "CARD", "NETBANKING", "WALLET"};

    private static final String[] PROVIDERS = {
            "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank", "Razorpay Gateway"
    };

    private static final String[] FAILURE_CODES = {
            "BANK_TIMEOUT",
            "NETWORK_ERROR",
            "INSUFFICIENT_FUNDS",
            "INVALID_PAYMENT_DETAILS",
            "PAYMENT_METHOD_ERROR",
            "TEMPORARY_PROVIDER_FAILURE"
    };

    // These are the failure codes that are considered "recoverable" via retry.
    // This mirrors what the Policy Engine will formally enforce in Phase 6 --
    // for now it just drives how we generate believable historical outcomes.
    private static final Set<String> RETRYABLE_CODES = new HashSet<>(Arrays.asList(
            "BANK_TIMEOUT", "NETWORK_ERROR", "TEMPORARY_PROVIDER_FAILURE"
    ));

    public DataSeederService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            System.out.println("[DataSeeder] Payments table already has data. Skipping seeding.");
            return;
        }

        List<Payment> payments = new ArrayList<>();
        int paymentNumber = 1001;

        for (int i = 0; i < TOTAL_RECORDS; i++) {
            String paymentId = "PAY" + paymentNumber++;
            payments.add(generateRandomPayment(paymentId));
        }

        paymentRepository.saveAll(payments);
        System.out.println("[DataSeeder] Seeded " + payments.size() + " synthetic payment records.");
    }

    private Payment generateRandomPayment(String paymentId) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setCustomerId("CUST" + (1000 + random.nextInt(9000)));
        payment.setAmount(randomAmount());
        payment.setPaymentMethod(randomFrom(PAYMENT_METHODS));
        payment.setProvider(randomFrom(PROVIDERS));
        payment.setCreatedAt(randomPastTimestamp());

        // 60% of payments succeed immediately -> nothing else to simulate.
        boolean succeedsImmediately = random.nextInt(100) < 60;
        if (succeedsImmediately) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setAttempts(1);
            payment.setFailureCode(null);
            return payment;
        }

        // Otherwise, this payment fails. Pick a realistic failure code.
        String failureCode = randomFrom(FAILURE_CODES);
        payment.setFailureCode(failureCode);
        boolean isRetryable = RETRYABLE_CODES.contains(failureCode);

        if (!isRetryable) {
            // Non-retryable failures (bad card details, insufficient funds) just
            // sit as FAILED -- retrying them without customer action wouldn't help.
            payment.setStatus(PaymentStatus.FAILED);
            payment.setAttempts(1);
            return payment;
        }

        // For retryable failures, split into three realistic buckets:
        // ~35% haven't been retried yet, ~40% were retried and recovered,
        // ~25% were retried and failed again.
        int bucket = random.nextInt(100);
        if (bucket < 35) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setAttempts(1);
        } else if (bucket < 75) {
            payment.setStatus(PaymentStatus.RECOVERED);
            payment.setAttempts(2);
        } else {
            payment.setStatus(PaymentStatus.FAILED_AGAIN);
            payment.setAttempts(2);
        }

        return payment;
    }

    private BigDecimal randomAmount() {
        // Random amount between ₹99 and ₹49,999, rounded to 2 decimal places.
        double amount = 99 + (random.nextDouble() * (49999 - 99));
        return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
    }

    private LocalDateTime randomPastTimestamp() {
        // Spread records over the last 30 days so charts/timelines look realistic.
        return LocalDateTime.now()
                .minusDays(random.nextInt(30))
                .minusMinutes(random.nextInt(24 * 60));
    }

    private String randomFrom(String[] options) {
        return options[random.nextInt(options.length)];
    }
}
