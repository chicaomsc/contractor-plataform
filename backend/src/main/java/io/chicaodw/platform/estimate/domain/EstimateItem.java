package io.chicaodw.platform.estimate.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A labour/service line within an {@link Estimate}. Description and unit price are copied
 * at creation time from the optional catalogue {@code serviceId} reference — later changes
 * to the catalogue must never retroactively change an existing estimate.
 */
@Entity
@Table(name = "estimate_items")
@Getter
@Setter
@NoArgsConstructor
public class EstimateItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false, updatable = false)
    private Estimate estimate;

    /** Optional, unenforced reference to the originating catalogue service — never re-read after creation. */
    @Column(name = "service_id")
    private UUID serviceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstimateUnit unit;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
