package io.chicaodw.platform.estimate.domain;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of the estimate module. Owns {@link EstimateItem} and {@link Material}
 * as child entities (cascade ALL, orphanRemoval) — they have no lifecycle outside an Estimate.
 *
 * currency, vatRate and upfrontPercentage are snapshots taken from Settings/Branding at
 * creation time: changing company configuration later must never retroactively change an
 * already-created estimate.
 */
@Entity
@Table(
        name = "estimates",
        uniqueConstraints = @UniqueConstraint(name = "uq_estimates_company_number", columnNames = {"company_id", "number"})
)
@Getter
@Setter
@NoArgsConstructor
public class Estimate extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // ── Customer snapshot (Sprint 10C) ──────────────────────────────────────
    // Frozen at creation / whenever customerId is reassigned. Never re-read from the
    // live Customer record after that — editing a Customer must not retroactively change
    // a document (PDF) already generated for this estimate. See ADR in the domain-model doc.

    @Column(name = "customer_name_snapshot", nullable = false)
    private String customerNameSnapshot;

    @Column(name = "customer_email_snapshot")
    private String customerEmailSnapshot;

    @Column(name = "customer_phone_snapshot", length = 50)
    private String customerPhoneSnapshot;

    @Column(name = "customer_tax_number_snapshot", length = 50)
    private String customerTaxNumberSnapshot;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "customer_address_street_snapshot")),
            @AttributeOverride(name = "city", column = @Column(name = "customer_address_city_snapshot")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "customer_address_postal_code_snapshot")),
            @AttributeOverride(name = "region", column = @Column(name = "customer_address_region_snapshot")),
            @AttributeOverride(name = "country", column = @Column(name = "customer_address_country_snapshot")),
    })
    private Address customerAddressSnapshot;

    @Column(nullable = false, length = 50)
    private String number;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstimateStatus status = EstimateStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Column(name = "estimated_duration_days")
    private Integer estimatedDurationDays;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    // ── Snapshots (never re-read from Settings/Branding after creation) ────────

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "upfront_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal upfrontPercentage;

    // ── Backend-calculated totals (never accepted from the client) ─────────────

    @Column(name = "labor_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborSubtotal = BigDecimal.ZERO;

    @Column(name = "material_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal materialSubtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "upfront_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal upfrontAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<EstimateItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<Material> materials = new ArrayList<>();

    public void addItem(EstimateItem item) {
        item.setEstimate(this);
        items.add(item);
    }

    public void addMaterial(Material material) {
        material.setEstimate(this);
        materials.add(material);
    }

    public void clearItems() {
        items.clear();
    }

    public void clearMaterials() {
        materials.clear();
    }
}
