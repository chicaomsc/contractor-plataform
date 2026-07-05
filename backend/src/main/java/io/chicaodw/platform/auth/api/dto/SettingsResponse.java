package io.chicaodw.platform.auth.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettingsResponse(
        UUID id,
        UUID companyId,
        String defaultCurrency,
        BigDecimal defaultTaxRate,
        Integer estimateValidityDays,
        String locale,
        String timezone
) {}
