package io.chicaodw.platform.estimate.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure domain service for estimate financial calculations. No Spring dependency, no
 * database access — every method is a deterministic function of its inputs, which keeps
 * it trivial to unit test.
 *
 * Monetary values (subtotals, VAT, totals) are scaled to 2 decimal places using
 * {@link RoundingMode#HALF_UP}. VAT and upfront percentages are applied at full precision
 * before the final rounding to 2 decimals, avoiding compounding rounding error across
 * the labor/material/VAT/upfront chain.
 */
public class EstimateCalculationService {

    public static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** quantity × unitPrice, rounded to money scale. */
    public BigDecimal calculateLineTotal(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(MONEY_SCALE, ROUNDING);
    }

    public EstimateTotals calculate(
            List<EstimateItem> items,
            List<Material> materials,
            BigDecimal vatRate,
            BigDecimal upfrontPercentage
    ) {
        BigDecimal laborSubtotal = sum(items.stream().map(EstimateItem::getTotal).toList());
        BigDecimal materialSubtotal = sum(materials.stream().map(Material::getTotal).toList());
        BigDecimal subtotal = laborSubtotal.add(materialSubtotal);

        BigDecimal vatAmount = percentageOf(subtotal, vatRate);
        BigDecimal total = subtotal.add(vatAmount);
        BigDecimal upfrontAmount = percentageOf(total, upfrontPercentage);
        BigDecimal remainingAmount = total.subtract(upfrontAmount);

        return new EstimateTotals(laborSubtotal, materialSubtotal, subtotal, vatAmount, total, upfrontAmount, remainingAmount);
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage)
                .divide(HUNDRED, MONEY_SCALE + 2, ROUNDING)
                .setScale(MONEY_SCALE, ROUNDING);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(MONEY_SCALE, ROUNDING);
    }
}
