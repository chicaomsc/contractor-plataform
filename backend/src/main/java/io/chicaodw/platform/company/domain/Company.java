package io.chicaodw.platform.company.domain;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "companies",
    uniqueConstraints = @UniqueConstraint(name = "uq_companies_slug", columnNames = "slug")
)
@Getter
@Setter
@NoArgsConstructor
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private String email;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(length = 50)
    private String phone;

    @Column(length = 50)
    private String whatsapp;

    @Column(length = 500)
    private String website;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(nullable = false, length = 2)
    private String country;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus status = CompanyStatus.ACTIVE;
}
