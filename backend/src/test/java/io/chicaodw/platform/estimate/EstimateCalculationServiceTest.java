package io.chicaodw.platform.estimate;

import io.chicaodw.platform.estimate.domain.EstimateCalculationService;
import io.chicaodw.platform.estimate.domain.EstimateItem;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.domain.Material;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstimateCalculationServiceTest {

    private final EstimateCalculationService calculationService = new EstimateCalculationService();

    // ── calculateLineTotal ───────────────────────────────────────────────────

    @Test
    void calculateLineTotal_multipliesQuantityByUnitPrice() {
        var total = calculationService.calculateLineTotal(new BigDecimal("3"), new BigDecimal("25.00"));

        assertThat(total).isEqualByComparingTo("75.00");
    }

    @Test
    void calculateLineTotal_roundsHalfUpToTwoDecimals() {
        // 3.333 * 10.005 = 33.346665 -> HALF_UP to 33.35
        var total = calculationService.calculateLineTotal(new BigDecimal("3.333"), new BigDecimal("10.005"));

        assertThat(total).isEqualByComparingTo("33.35");
    }

    // ── calculate — full pipeline ───────────────────────────────────────────

    @Test
    void calculate_withoutMaterials_computesLaborOnly() {
        var items = List.of(item("100.00"), item("50.00"));

        var totals = calculationService.calculate(items, List.of(), new BigDecimal("23"), new BigDecimal("50"));

        assertThat(totals.laborSubtotal()).isEqualByComparingTo("150.00");
        assertThat(totals.materialSubtotal()).isEqualByComparingTo("0.00");
        assertThat(totals.subtotal()).isEqualByComparingTo("150.00");
        assertThat(totals.vatAmount()).isEqualByComparingTo("34.50");
        assertThat(totals.total()).isEqualByComparingTo("184.50");
        assertThat(totals.upfrontAmount()).isEqualByComparingTo("92.25");
        assertThat(totals.remainingAmount()).isEqualByComparingTo("92.25");
    }

    @Test
    void calculate_withoutLabor_computesMaterialOnly() {
        var materials = List.of(material("80.00"), material("20.00"));

        var totals = calculationService.calculate(List.of(), materials, new BigDecimal("23"), new BigDecimal("50"));

        assertThat(totals.laborSubtotal()).isEqualByComparingTo("0.00");
        assertThat(totals.materialSubtotal()).isEqualByComparingTo("100.00");
        assertThat(totals.subtotal()).isEqualByComparingTo("100.00");
        assertThat(totals.vatAmount()).isEqualByComparingTo("23.00");
        assertThat(totals.total()).isEqualByComparingTo("123.00");
    }

    @Test
    void calculate_withLaborAndMaterials_sumsBoth() {
        var items = List.of(item("150.00"));
        var materials = List.of(material("100.00"));

        var totals = calculationService.calculate(items, materials, new BigDecimal("23"), new BigDecimal("50"));

        assertThat(totals.laborSubtotal()).isEqualByComparingTo("150.00");
        assertThat(totals.materialSubtotal()).isEqualByComparingTo("100.00");
        assertThat(totals.subtotal()).isEqualByComparingTo("250.00");
        assertThat(totals.vatAmount()).isEqualByComparingTo("57.50");
        assertThat(totals.total()).isEqualByComparingTo("307.50");
        assertThat(totals.upfrontAmount()).isEqualByComparingTo("153.75");
        assertThat(totals.remainingAmount()).isEqualByComparingTo("153.75");
    }

    @Test
    void calculate_vatZero_totalEqualsSubtotal() {
        var items = List.of(item("200.00"));

        var totals = calculationService.calculate(items, List.of(), BigDecimal.ZERO, new BigDecimal("50"));

        assertThat(totals.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(totals.total()).isEqualByComparingTo("200.00");
    }

    @Test
    void calculate_upfrontZero_remainingEqualsTotal() {
        var items = List.of(item("200.00"));

        var totals = calculationService.calculate(items, List.of(), new BigDecimal("23"), BigDecimal.ZERO);

        assertThat(totals.upfrontAmount()).isEqualByComparingTo("0.00");
        assertThat(totals.remainingAmount()).isEqualByComparingTo(totals.total());
    }

    @Test
    void calculate_upfrontHundred_remainingIsZero() {
        var items = List.of(item("200.00"));

        var totals = calculationService.calculate(items, List.of(), new BigDecimal("23"), new BigDecimal("100"));

        assertThat(totals.upfrontAmount()).isEqualByComparingTo(totals.total());
        assertThat(totals.remainingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_noItemsOrMaterials_allZero() {
        var totals = calculationService.calculate(List.of(), List.of(), new BigDecimal("23"), new BigDecimal("50"));

        assertThat(totals.subtotal()).isEqualByComparingTo("0.00");
        assertThat(totals.total()).isEqualByComparingTo("0.00");
        assertThat(totals.upfrontAmount()).isEqualByComparingTo("0.00");
        assertThat(totals.remainingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_roundingAppliedOnlyAtFinalStep() {
        // subtotal 99.99, VAT 23% -> 22.9977 -> rounds to 23.00
        var items = List.of(item("99.99"));

        var totals = calculationService.calculate(items, List.of(), new BigDecimal("23"), new BigDecimal("50"));

        assertThat(totals.vatAmount()).isEqualByComparingTo("23.00");
        assertThat(totals.total()).isEqualByComparingTo("122.99");
    }

    private static EstimateItem item(String total) {
        var item = new EstimateItem();
        item.setTotal(new BigDecimal(total));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal(total));
        item.setUnit(EstimateUnit.UNIT);
        item.setDescription("Item");
        return item;
    }

    private static Material material(String total) {
        var material = new Material();
        material.setTotal(new BigDecimal(total));
        material.setQuantity(BigDecimal.ONE);
        material.setUnitPrice(new BigDecimal(total));
        material.setUnit(EstimateUnit.UNIT);
        material.setName("Material");
        return material;
    }
}
