package io.chicaodw.platform.estimate.pdf;

import java.util.List;

/**
 * Immutable, renderer-agnostic model of an estimate PDF. Every text field here is already
 * resolved/formatted — the renderer draws exactly what it's given and performs no lookups,
 * no currency/date formatting, and no arithmetic.
 */
public record EstimatePdfDocument(
        SellerInfo seller,
        EstimateMetadata metadata,
        CustomerSnapshot customer,
        List<LineItem> items,
        List<LineItem> materials,
        FinancialSummary summary,
        String notes,
        String terms,
        boolean draft,
        boolean cancelled
) {

    /** Company/Branding data as of generation time (not snapshotted — see ADR in domain-model.md). */
    public record SellerInfo(
            String displayName,
            String legalName,
            String taxNumber,
            String phone,
            String email,
            String website,
            String addressLine,
            byte[] logo,
            String primaryColorHex
    ) {}

    public record EstimateMetadata(
            String number,
            String statusLabel,
            String issueDateLabel,
            String validUntilLabel,
            String expectedStartDateLabel,
            String estimatedDurationLabel,
            String title,
            String description
    ) {}

    /** Frozen at estimate creation/reassignment — never the customer's current live data. */
    public record CustomerSnapshot(
            String name,
            String taxNumber,
            String phone,
            String email,
            String addressLine
    ) {}

    public record LineItem(
            String description,
            String quantityLabel,
            String unitLabel,
            String unitPriceLabel,
            String totalLabel
    ) {}

    /** Every value here is copied verbatim from the persisted Estimate — nothing is computed. */
    public record FinancialSummary(
            String currency,
            String laborSubtotalLabel,
            String materialSubtotalLabel,
            String subtotalLabel,
            String vatLabel,
            String vatAmountLabel,
            String totalLabel,
            String upfrontLabel,
            String upfrontAmountLabel,
            String remainingLabel
    ) {}
}
