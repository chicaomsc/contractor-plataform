package io.chicaodw.platform.servicecatalog;

import io.chicaodw.platform.servicecatalog.application.ServiceSlugGenerator;
import io.chicaodw.platform.servicecatalog.infrastructure.persistence.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceSlugGeneratorTest {

    @Mock ServiceRepository serviceRepository;
    @InjectMocks ServiceSlugGenerator slugGenerator;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
    }

    // ── Basic slug generation ─────────────────────────────────────────────────

    @Test
    void generate_simpleAsciiName_producesLowercaseHyphenated() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "house-painting")).thenReturn(false);

        String slug = slugGenerator.generate("House Painting", companyId);

        assertThat(slug).isEqualTo("house-painting");
    }

    @Test
    void generate_nameWithAccents_stripsAccents() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "construcao-civil")).thenReturn(false);

        String slug = slugGenerator.generate("Construção Civil", companyId);

        assertThat(slug).isEqualTo("construcao-civil");
    }

    @Test
    void generate_nameWithSpecialChars_removesNonAlphanumeric() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura-interior-exterior")).thenReturn(false);

        String slug = slugGenerator.generate("Pintura Interior & Exterior!", companyId);

        assertThat(slug).isEqualTo("pintura-interior-exterior");
    }

    @Test
    void generate_nameWithMultipleSpaces_collapsesToSingleHyphen() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "renovacao-predial")).thenReturn(false);

        String slug = slugGenerator.generate("Renovação  Predial", companyId);

        assertThat(slug).isEqualTo("renovacao-predial");
    }

    // ── Collision resolution ──────────────────────────────────────────────────

    @Test
    void generate_slugExists_appendsCounter() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura")).thenReturn(true);
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura-2")).thenReturn(false);

        String slug = slugGenerator.generate("Pintura", companyId);

        assertThat(slug).isEqualTo("pintura-2");
    }

    @Test
    void generate_firstTwoExist_returnsThird() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura")).thenReturn(true);
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura-2")).thenReturn(true);
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura-3")).thenReturn(false);

        String slug = slugGenerator.generate("Pintura", companyId);

        assertThat(slug).isEqualTo("pintura-3");
    }

    @Test
    void generate_differentCompanies_noCollisionBetweenThem() {
        UUID otherCompanyId = UUID.randomUUID();
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "pintura")).thenReturn(false);
        when(serviceRepository.existsByCompanyIdAndSlug(otherCompanyId, "pintura")).thenReturn(false);

        String slug1 = slugGenerator.generate("Pintura", companyId);
        String slug2 = slugGenerator.generate("Pintura", otherCompanyId);

        assertThat(slug1).isEqualTo("pintura");
        assertThat(slug2).isEqualTo("pintura");
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void generate_nameWithLeadingTrailingSpaces_trimmed() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "limpeza")).thenReturn(false);

        String slug = slugGenerator.generate("  Limpeza  ", companyId);

        assertThat(slug).isEqualTo("limpeza");
    }

    @Test
    void generate_mixedCaseName_lowercaseResult() {
        when(serviceRepository.existsByCompanyIdAndSlug(companyId, "impermeabilizacao")).thenReturn(false);

        String slug = slugGenerator.generate("IMPERMEABILIZAÇÃO", companyId);

        assertThat(slug).isEqualTo("impermeabilizacao");
    }
}
