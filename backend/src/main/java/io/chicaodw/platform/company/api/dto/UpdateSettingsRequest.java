package io.chicaodw.platform.company.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateSettingsRequest(
        @Size(min = 3, max = 3)                          String defaultCurrency,
        @DecimalMin("0") @DecimalMax("100")              BigDecimal defaultTaxRate,
        @Min(1)                                          Integer estimateValidityDays,
        @Size(max = 2000)                                String estimateFooterText,
        @Size(max = 10)                                  String locale,
        @Size(max = 50)                                  String timezone,
        @Size(max = 50)                                  String dateFormat,
        @Size(max = 50)                                  String numberFormat,
        @DecimalMin("0") @DecimalMax("100")              BigDecimal upfrontPercentage
) {}
