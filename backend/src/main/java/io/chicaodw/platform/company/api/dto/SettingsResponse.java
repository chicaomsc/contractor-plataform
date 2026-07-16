package io.chicaodw.platform.company.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettingsResponse(
        UUID       id,
        UUID       companyId,
        String     defaultCurrency,
        BigDecimal defaultTaxRate,
        Integer    estimateValidityDays,
        String     estimateFooterText,
        String     locale,
        String     timezone,
        String     dateFormat,
        String     numberFormat,
        BigDecimal upfrontPercentage
) {}
