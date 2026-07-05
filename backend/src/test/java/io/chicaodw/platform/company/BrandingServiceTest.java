package io.chicaodw.platform.company;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.application.BrandingService;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandingServiceTest {

    @Mock BrandingRepository brandingRepository;
    @Mock CompanyMapper      companyMapper;

    @InjectMocks BrandingService brandingService;

    private UUID     companyId;
    private Branding branding;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        branding  = new Branding();
        branding.setCompanyId(companyId);
        branding.setPrimaryColor("#1E40AF");
        branding.setSecondaryColor("#3B82F6");
        branding.setAccentColor("#F59E0B");
        ReflectionTestUtils.setField(branding, "id", UUID.randomUUID());
    }

    // ── getBranding ───────────────────────────────────────────────────────────

    @Test
    void getBranding_returnsMappedResponse() {
        var expected = brandingResponse(companyId, "#1E40AF");
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(companyMapper.toBrandingResponse(branding)).thenReturn(expected);

        BrandingResponse result = brandingService.getBranding(companyId);

        assertThat(result.primaryColor()).isEqualTo("#1E40AF");
    }

    @Test
    void getBranding_notFound_throwsException() {
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandingService.getBranding(companyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateBranding ────────────────────────────────────────────────────────

    @Test
    void updateBranding_fullUpdate_persistsAllFields() {
        var request  = new UpdateBrandingRequest("#FF0000", "#00FF00", "#0000FF",
                "Tagline atualizada", "Sobre nós", "Rodapé", "ORC", "João Silva");
        var expected = brandingResponse(companyId, "#FF0000");

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(companyMapper.toBrandingResponse(branding)).thenReturn(expected);

        BrandingResponse result = brandingService.updateBranding(companyId, request);

        assertThat(result.primaryColor()).isEqualTo("#FF0000");
    }

    @Test
    void updateBranding_partialUpdate_onlyChangesProvidedFields() {
        var request  = new UpdateBrandingRequest(null, null, null, "Novo tagline", null, null, null, null);
        var expected = brandingResponse(companyId, "#1E40AF");

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(companyMapper.toBrandingResponse(branding)).thenReturn(expected);

        brandingService.updateBranding(companyId, request);

        // primaryColor was not in the request → must be unchanged
        assertThat(branding.getPrimaryColor()).isEqualTo("#1E40AF");
        assertThat(branding.getTagline()).isEqualTo("Novo tagline");
    }

    @Test
    void updateBranding_notFound_throwsException() {
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandingService.updateBranding(companyId,
                new UpdateBrandingRequest(null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static BrandingResponse brandingResponse(UUID companyId, String primary) {
        return new BrandingResponse(UUID.randomUUID(), companyId, null,
                primary, null, null, null, null, null, null, null);
    }
}
