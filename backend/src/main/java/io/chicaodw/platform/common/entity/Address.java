package io.chicaodw.platform.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_street")
    private String street;

    @Column(name = "address_city")
    private String city;

    @Column(name = "address_postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_region")
    private String region;

    @Column(name = "address_country", length = 2)
    private String country;
}
