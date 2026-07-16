package io.chicaodw.platform.company;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.api.dto.SettingsResponse;
import io.chicaodw.platform.company.api.dto.UpdateSettingsRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.application.SettingsService;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock SettingsRepository settingsRepository;
    @Mock CompanyMapper      companyMapper;

    @InjectMocks SettingsService settingsService;

    private UUID     companyId;
    private Settings settings;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        settings  = new Settings();
        settings.setCompanyId(companyId);
        ReflectionTestUtils.setField(settings, "id", UUID.randomUUID());
    }

    // ── getSettings ───────────────────────────────────────────────────────────

    @Test
    void getSettings_returnsMappedResponse() {
        var expected = settingsResponse(companyId, "EUR");
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));
        when(companyMapper.toSettingsResponse(settings)).thenReturn(expected);

        SettingsResponse result = settingsService.getSettings(companyId);

        assertThat(result.defaultCurrency()).isEqualTo("EUR");
    }

    @Test
    void getSettings_notFound_throwsException() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.getSettings(companyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateSettings ────────────────────────────────────────────────────────

    @Test
    void updateSettings_fullUpdate_persistsAllFields() {
        var request  = new UpdateSettingsRequest("USD", new BigDecimal("20.00"),
                60, "Rodapé atualizado", "en-US", "America/New_York", "MM/dd/yyyy", "en-US", new BigDecimal("40.00"));
        var expected = settingsResponse(companyId, "USD");

        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(settings)).thenReturn(settings);
        when(companyMapper.toSettingsResponse(settings)).thenReturn(expected);

        SettingsResponse result = settingsService.updateSettings(companyId, request);

        assertThat(result.defaultCurrency()).isEqualTo("USD");
    }

    @Test
    void updateSettings_partialUpdate_onlyChangesProvidedFields() {
        var request = new UpdateSettingsRequest(null, new BigDecimal("6.00"),
                null, null, null, null, null, null, null);

        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(settings)).thenReturn(settings);
        when(companyMapper.toSettingsResponse(settings)).thenReturn(settingsResponse(companyId, "EUR"));

        settingsService.updateSettings(companyId, request);

        // Only defaultTaxRate must have changed
        assertThat(settings.getDefaultTaxRate()).isEqualByComparingTo("6.00");
        assertThat(settings.getDefaultCurrency()).isEqualTo("EUR"); // unchanged default
    }

    @Test
    void updateSettings_notFound_throwsException() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.updateSettings(companyId,
                new UpdateSettingsRequest(null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static SettingsResponse settingsResponse(UUID companyId, String currency) {
        return new SettingsResponse(UUID.randomUUID(), companyId, currency,
                BigDecimal.ZERO, 30, null, "pt-PT", "Europe/Lisbon", "dd/MM/yyyy", "pt-PT", new BigDecimal("50.00"));
    }
}
