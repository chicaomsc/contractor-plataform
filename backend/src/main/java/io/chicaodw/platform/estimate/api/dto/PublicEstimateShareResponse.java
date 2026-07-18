package io.chicaodw.platform.estimate.api.dto;

import java.util.List;

/**
 * Read-only, unauthenticated view of a shared estimate. Every field is display-ready
 * (already formatted — see EstimatePdfDocumentFactory, which this is assembled from) and
 * scoped to what a customer should see: no companyId, no estimateId, no internal IDs, no
 * administrative fields.
 */
public record PublicEstimateShareResponse(
        Seller seller,
        EstimateInfo estimate,
        Customer customer,
        List<LineItem> items,
        List<LineItem> materials,
        FinancialSummary summary,
        String notes,
        String terms
) {

    public record Seller(
            String displayName,
            String logoUrl,
            String phone,
            String email,
            String website,
            String addressLine
    ) {}

    public record EstimateInfo(
            String number,
            String title,
            String description,
            String status,
            boolean draft,
            boolean cancelled,
            String issueDate,
            String validUntil
    ) {}

    public record Customer(
            String name
    ) {}

    public record LineItem(
            String description,
            String quantity,
            String unit,
            String unitPrice,
            String total
    ) {}

    public record FinancialSummary(
            String currency,
            String laborSubtotal,
            String materialSubtotal,
            String subtotal,
            String vatLabel,
            String vatAmount,
            String total,
            String upfrontLabel,
            String upfrontAmount,
            String remaining
    ) {}
}
