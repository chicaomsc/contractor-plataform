package io.chicaodw.platform.company.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "brandings",
    uniqueConstraints = @UniqueConstraint(name = "uq_brandings_company", columnNames = "company_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Branding extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(length = 500)
    private String tagline;

    @Column(name = "accent_color", length = 7)
    private String accentColor;

    @Column(name = "about_text", columnDefinition = "TEXT")
    private String aboutText;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "quotation_prefix", length = 20)
    private String quotationPrefix;

    @Column(name = "signature_name")
    private String signatureName;
}
