package com.payrecover.payrecoverai.dto;

import java.math.BigDecimal;

/**
 * A single slice of a chart.
 *
 * One generic shape reused for any "group by X and count" chart, e.g.
 * grouping failed payments by failure code:
 *   { "label": "BANK_TIMEOUT", "count": 9, "amount": 84210.50, "percentage": 28.13 }
 *
 * Keeping this generic means the React side can render a pie/bar chart from
 * ANY endpoint that returns List<BreakdownItemDto> using the same component.
 */
public class BreakdownItemDto {

    private String label;
    private long count;
    private BigDecimal amount;   // total money sitting in this slice
    private double percentage;   // share of the whole, e.g. 28.13

    public BreakdownItemDto() {
    }

    public BreakdownItemDto(String label, long count, BigDecimal amount, double percentage) {
        this.label = label;
        this.count = count;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
