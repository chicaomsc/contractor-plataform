package io.chicaodw.platform.company.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "settings",
    uniqueConstraints = @UniqueConstraint(name = "uq_settings_company", columnNames = "company_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Settings extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "EUR";

    @Column(name = "default_tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultTaxRate = BigDecimal.ZERO;

    @Column(name = "estimate_validity_days", nullable = false)
    private Integer estimateValidityDays = 30;

    @Column(name = "estimate_footer_text", columnDefinition = "TEXT")
    private String estimateFooterText;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "pt-PT";

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Europe/Lisbon";

    @Column(name = "date_format", nullable = false, length = 50)
    private String dateFormat = "dd/MM/yyyy";

    @Column(name = "number_format", nullable = false, length = 50)
    private String numberFormat = "pt-PT";

    @Column(name = "upfront_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal upfrontPercentage = new BigDecimal("50.00");
}
