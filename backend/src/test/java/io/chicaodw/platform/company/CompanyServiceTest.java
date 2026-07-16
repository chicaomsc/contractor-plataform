package io.chicaodw.platform.company;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.storage.ImageUploadPolicy;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.CompanyResponse;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.company.api.mapper.CompanyMapper;
import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository  companyRepository;
    @Mock BrandingRepository brandingRepository;
    @Mock SettingsRepository settingsRepository;
    @Mock StorageService     storageService;
    @Spy ImageUploadPolicy   imageUploadPolicy = new ImageUploadPolicy();
    @Mock CompanyMapper      companyMapper;

    @InjectMocks CompanyService companyService;

    private UUID    companyId;
    private Company company;
    private Branding branding;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();

        company = new Company();
        company.setName("Obras Lda");
        company.setSlug("obras-lda");
        company.setEmail("info@obras.pt");
        company.setPhone("+351 910 000 000");
        company.setCountry("PT");
        company.setStatus(CompanyStatus.ACTIVE);
        ReflectionTestUtils.setField(company, "id", companyId);

        branding = new Branding();
        branding.setCompanyId(companyId);
        branding.setPrimaryColor("#1E40AF");
        ReflectionTestUtils.setField(branding, "id", UUID.randomUUID());
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsCompanyResponse() {
        var expected = new CompanyResponse(companyId, "Obras Lda", null, "obras-lda",
                "info@obras.pt", null, null, null, null, "PT", null, "ACTIVE");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyMapper.toCompanyResponse(company)).thenReturn(expected);

        CompanyResponse result = companyService.getProfile(companyId);

        assertThat(result.name()).isEqualTo("Obras Lda");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_fullUpdate_persistsAllFields() {
        var request = new UpdateCompanyRequest(
                "Nova Obras", "NovaTrade", "nova@obras.pt",
                "+351 920", "+351 930", "https://nova.pt", "PT123456",
                "PT", null
        );
        var expected = new CompanyResponse(companyId, "Nova Obras", "NovaTrade", "obras-lda",
                "nova@obras.pt", "+351 920", "+351 930", "https://nova.pt", "PT123456", "PT", null, "ACTIVE");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toCompanyResponse(company)).thenReturn(expected);

        CompanyResponse result = companyService.updateProfile(companyId, request);

        assertThat(result.tradeName()).isEqualTo("NovaTrade");
        assertThat(result.website()).isEqualTo("https://nova.pt");
    }

    @Test
    void updateProfile_partialUpdate_onlyChangesProvidedFields() {
        var request = new UpdateCompanyRequest(
                "Nome Actualizado", null, null, null, null, null, null, null, null
        );
        var expected = new CompanyResponse(companyId, "Nome Actualizado", null, "obras-lda",
                "info@obras.pt", null, null, null, null, "PT", null, "ACTIVE");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toCompanyResponse(company)).thenReturn(expected);

        CompanyResponse result = companyService.updateProfile(companyId, request);

        // email was NOT in the request → stays as-is
        assertThat(result.name()).isEqualTo("Nome Actualizado");
        assertThat(result.email()).isEqualTo("info@obras.pt");
    }

    @Test
    void updateProfile_unknownCompany_throwsNotFound() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateProfile(companyId,
                new UpdateCompanyRequest("x", null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── uploadLogo ────────────────────────────────────────────────────────────

    @Test
    void uploadLogo_validImage_savesFileAndUpdatesLogoUrl() {
        var file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes());
        var expected = new BrandingResponse(branding.getId(), companyId, "/uploads/company/" + companyId + "/logo/abc.png",
                "#1E40AF", null, null, null, null, null, null, null);

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(storageService.store(anyString(), any())).thenReturn("/uploads/company/" + companyId + "/logo/abc.png");
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(companyMapper.toBrandingResponse(branding)).thenReturn(expected);

        BrandingResponse result = companyService.uploadLogo(companyId, file);

        assertThat(result.logoUrl()).contains("/uploads/");
        verify(storageService).store("company/" + companyId + "/logo", file);
    }

    @Test
    void uploadLogo_emptyFile_throwsBusinessRule() {
        var empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> companyService.uploadLogo(companyId, empty))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadLogo_nonImageFile_throwsBusinessRule() {
        var pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> companyService.uploadLogo(companyId, pdf))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("image");
    }

    @Test
    void uploadLogo_existingLogo_deletesOldFile() {
        branding.setLogoUrl("/uploads/company/" + companyId + "/logo/old.png");
        var file = new MockMultipartFile("file", "new.png", "image/png", pngBytes());

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(storageService.store(anyString(), any())).thenReturn("/uploads/company/" + companyId + "/logo/new.png");
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(companyMapper.toBrandingResponse(branding)).thenReturn(
                new BrandingResponse(branding.getId(), companyId, "/uploads/company/" + companyId + "/logo/new.png",
                        null, null, null, null, null, null, null, null));

        companyService.uploadLogo(companyId, file);

        verify(storageService).delete("/uploads/company/" + companyId + "/logo/old.png");
    }

    // ── deleteLogo ────────────────────────────────────────────────────────────

    @Test
    void deleteLogo_withExistingLogo_deletesFileAndClearsUrl() {
        branding.setLogoUrl("/uploads/company/" + companyId + "/logo/logo.png");

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(companyMapper.toBrandingResponse(branding)).thenReturn(
                new BrandingResponse(branding.getId(), companyId, null,
                        null, null, null, null, null, null, null, null));

        BrandingResponse result = companyService.deleteLogo(companyId);

        verify(storageService).delete("/uploads/company/" + companyId + "/logo/logo.png");
        assertThat(result.logoUrl()).isNull();
    }

    @Test
    void deleteLogo_withNoLogo_doesNotCallStorage() {
        branding.setLogoUrl(null);

        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(companyMapper.toBrandingResponse(branding)).thenReturn(
                new BrandingResponse(branding.getId(), companyId, null,
                        null, null, null, null, null, null, null, null));

        companyService.deleteLogo(companyId);

        verify(storageService, never()).delete(anyString());
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };
    }
}
