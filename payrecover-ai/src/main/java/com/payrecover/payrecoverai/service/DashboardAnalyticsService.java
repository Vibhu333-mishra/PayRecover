package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.BreakdownItemDto;
import com.payrecover.payrecoverai.dto.DailyTrendItemDto;
import com.payrecover.payrecoverai.dto.DashboardSummaryDto;
import com.payrecover.payrecoverai.dto.MethodBreakdownItemDto;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PHASE 4 -- chart data for the dashboard.
 *
 * WHAT IT DOES
 * PaymentService.getDashboardMetrics() already gives us the seven headline
 * numbers (the "cards"). This class produces the *grouped* data that charts
 * need: failures per failure-code, health per payment method, and a day-by-day
 * trend line.
 *
 * WHY A SEPARATE CLASS
 * PaymentService is about individual payments. Aggregation/reporting is a
 * different responsibility, so it lives in its own class. It also means Phase 4
 * added zero risk to the Phase 1-3 code that already works.
 *
 * WHY GROUP IN JAVA INSTEAD OF SQL
 * We only ever hold ~80-100 demo rows. Loading them once and grouping with a
 * plain HashMap is easier to read, easier to debug, and easier for you to
 * explain in a demo than a GROUP BY JPQL query. If this were a real product
 * with millions of rows you would push the grouping into SQL instead -- that is
 * a good thing to say out loud to a judge.
 */
@Service
public class DashboardAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public DashboardAnalyticsService(PaymentRepository paymentRepository,
                                     PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    /** Default window for the trend chart. */
    public static final int DEFAULT_TREND_DAYS = 14;

    /**
     * Groups every payment that failed at least once by its raw failure code.
     * Sorted so the biggest problem is first -- that is what a merchant wants
     * to see at the top of the chart.
     */
    public List<BreakdownItemDto> getFailureBreakdown() {
        List<Payment> everFailed = paymentRepository.findAll().stream()
                .filter(this::hasFailedAtLeastOnce)
                .toList();

        // code -> running count, and code -> running money total
        Map<String, Long> countByCode = new HashMap<>();
        Map<String, BigDecimal> amountByCode = new HashMap<>();

        for (Payment p : everFailed) {
            String code = p.getFailureCode() == null ? "UNKNOWN" : p.getFailureCode();
            // merge(): if the key is absent put 1L, otherwise add 1 to what's there.
            countByCode.merge(code, 1L, Long::sum);
            amountByCode.merge(code, p.getAmount(), BigDecimal::add);
        }

        long totalFailed = everFailed.size();

        List<BreakdownItemDto> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByCode.entrySet()) {
            String code = entry.getKey();
            long count = entry.getValue();
            double percentage = totalFailed == 0 ? 0.0 : (count * 100.0) / totalFailed;
            result.add(new BreakdownItemDto(code, count, amountByCode.get(code), round2(percentage)));
        }

        result.sort(Comparator.comparingLong(BreakdownItemDto::getCount).reversed());
        return result;
    }

    /**
     * Per-method health: how many payments used this method, how many failed,
     * how many were recovered, and the resulting failure rate.
     */
    public List<MethodBreakdownItemDto> getMethodBreakdown() {
        List<Payment> all = paymentRepository.findAll();

        Map<String, long[]> stats = new HashMap<>(); // [total, failed, recovered]

        for (Payment p : all) {
            String method = p.getPaymentMethod();
            // computeIfAbsent(): create the counter array the first time we see this method.
            long[] counters = stats.computeIfAbsent(method, key -> new long[3]);
            counters[0]++;
            if (hasFailedAtLeastOnce(p)) {
                counters[1]++;
            }
            if (p.getStatus() == PaymentStatus.RECOVERED) {
                counters[2]++;
            }
        }

        List<MethodBreakdownItemDto> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : stats.entrySet()) {
            long total = entry.getValue()[0];
            long failed = entry.getValue()[1];
            long recovered = entry.getValue()[2];
            double failureRate = total == 0 ? 0.0 : (failed * 100.0) / total;
            result.add(new MethodBreakdownItemDto(
                    entry.getKey(), total, failed, recovered, round2(failureRate)));
        }

        result.sort(Comparator.comparingDouble(MethodBreakdownItemDto::getFailureRate).reversed());
        return result;
    }

    /**
     * Day-by-day totals for the last {@code days} days, oldest first.
     * Days with no payments are included with zeros so the line chart's x-axis
     * stays evenly spaced.
     */
    public List<DailyTrendItemDto> getDailyTrend(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("days must be at least 1");
        }
        if (days > 90) {
            throw new IllegalArgumentException("days must be 90 or less");
        }

        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(days - 1L);

        Map<LocalDate, long[]> byDate = new HashMap<>(); // [total, failed, recovered]

        for (Payment p : paymentRepository.findAll()) {
            LocalDate date = p.getCreatedAt().toLocalDate();
            if (date.isBefore(windowStart) || date.isAfter(today)) {
                continue; // outside the requested window
            }
            long[] counters = byDate.computeIfAbsent(date, key -> new long[3]);
            counters[0]++;
            if (hasFailedAtLeastOnce(p)) {
                counters[1]++;
            }
            if (p.getStatus() == PaymentStatus.RECOVERED) {
                counters[2]++;
            }
        }

        List<DailyTrendItemDto> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = windowStart.plusDays(i);
            long[] counters = byDate.getOrDefault(date, new long[3]);
            result.add(new DailyTrendItemDto(date, counters[0], counters[1], counters[2]));
        }
        return result;
    }

    /** Everything the dashboard screen needs, in a single response. */
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setMetrics(paymentService.getDashboardMetrics());
        summary.setFailureBreakdown(getFailureBreakdown());
        summary.setMethodBreakdown(getMethodBreakdown());
        summary.setDailyTrend(getDailyTrend(DEFAULT_TREND_DAYS));
        return summary;
    }

    /**
     * "Failed at least once" is the honest definition of a revenue leak:
     * FAILED (not retried yet), FAILED_AGAIN (retry also failed), and
     * RECOVERED (it failed first, we just won it back).
     */
    private boolean hasFailedAtLeastOnce(Payment payment) {
        return payment.getStatus() != PaymentStatus.SUCCESS;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
