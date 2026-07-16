package io.chicaodw.platform.estimate.domain;

import java.math.BigDecimal;

public record EstimateTotals(
        BigDecimal laborSubtotal,
        BigDecimal materialSubtotal,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal total,
        BigDecimal upfrontAmount,
        BigDecimal remainingAmount
) {}
