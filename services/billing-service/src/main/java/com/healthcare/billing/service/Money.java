package com.healthcare.billing.service;

import com.healthcare.billing.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary calculation policy for the Billing Service.
 *
 * <p><b>Policy</b>:
 * <ul>
 *   <li>Storage: {@code NUMERIC(19,4)} — 4 fractional digits.</li>
 *   <li>Calculations: {@link RoundingMode#HALF_UP} at scale 4.</li>
 *   <li>Display: scale 2 (handled at the API / presentation layer
 *       when necessary).</li>
 *   <li>All comparisons use {@link BigDecimal#compareTo} rather than
 *       {@code equals} so that values which differ only in scale
 *       ({@code 1.00} vs {@code 1.0000}) are treated as equal.</li>
 * </ul>
 *
 * <p>This is the single source of truth for money math in the
 * Billing Service. The same policy is applied to line items, totals,
 * and payment amounts.
 */
public final class Money {

    /** Internal scale for calculations and storage. */
    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() { /* utility */ }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    /**
     * Compute a line total: {@code quantity * unitPrice}, scaled and
     * rounded per the policy.
     */
    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        if (quantity < 0) {
            throw new InvalidAmountException("quantity must be >= 0");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidAmountException("unitPrice must be >= 0");
        }
        return unitPrice.setScale(SCALE, ROUNDING)
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Sum a list of line totals, scaling the result per the policy.
     * Returns zero for an empty or null list.
     */
    public static BigDecimal sum(java.util.List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return zero();
        BigDecimal total = zero();
        for (BigDecimal v : values) {
            if (v == null) continue;
            total = total.add(v.setScale(SCALE, ROUNDING));
        }
        return total.setScale(SCALE, ROUNDING);
    }

    /** True if {@code a >= b} in money terms. */
    public static boolean gte(BigDecimal a, BigDecimal b) {
        return a.setScale(SCALE, ROUNDING).compareTo(b.setScale(SCALE, ROUNDING)) >= 0;
    }

    public static BigDecimal requirePositive(BigDecimal amount, String name) {
        if (amount == null) {
            throw new InvalidAmountException(name + " is required");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException(name + " must be > 0");
        }
        return amount.setScale(SCALE, ROUNDING);
    }
}
