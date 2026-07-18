package io.chicaodw.platform.estimate;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.estimate.application.EstimatePdfService;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimatePdfServiceTest {

    @Mock EstimateRepository estimateRepository;
    @Mock CompanyRepository companyRepository;
    @Mock BrandingRepository brandingRepository;
    @Mock SettingsRepository settingsRepository;
    @Mock StorageService storageService;

    EstimatePdfService pdfService;

    private UUID companyId;
    private UUID estimateId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        estimateId = UUID.randomUUID();
        pdfService = new EstimatePdfService(
                estimateRepository, companyRepository, brandingRepository, settingsRepository, storageService);
    }

    @Test
    void generatePdf_ownEstimate_returnsValidPdfWithExpectedFilename() throws Exception {
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId))
                .thenReturn(Optional.of(estimate("ORC-2026-0001")));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        var result = pdfService.generatePdf(companyId, estimateId);

        assertThat(result.filename()).isEqualTo("orcamento-ORC-2026-0001.pdf");
        assertThat(result.bytes()).isNotEmpty();
        assertThat(extractText(result.bytes())).contains("ORC-2026-0001");
    }

    @Test
    void generatePdf_unknownEstimate_throwsResourceNotFoundException() {
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generatePdf(companyId, estimateId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generatePdf_estimateBelongsToAnotherCompany_throwsResourceNotFoundException() {
        // The repository query itself is scoped by (id, companyId) — a cross-tenant id simply
        // never matches, mirroring how every other estimate lookup in this codebase behaves.
        UUID otherCompanyId = UUID.randomUUID();
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, otherCompanyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generatePdf(otherCompanyId, estimateId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(estimateRepository).findDetailByIdAndCompanyId(estimateId, otherCompanyId);
        verifyNoMoreInteractions(companyRepository, brandingRepository, settingsRepository, storageService);
    }

    @Test
    void generatePdf_doesNotChangeEstimateStatus_orPersistAnything() {
        var estimate = estimate("ORC-2026-0001");
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        pdfService.generatePdf(companyId, estimateId);

        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.DRAFT);
        verify(estimateRepository, never()).save(any());
    }

    @Test
    void generatePdf_missingLogo_degradesGracefully_stillReturnsPdf() {
        var branding = new Branding();
        branding.setLogoUrl("/uploads/company/x/logo/missing.png");
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId))
                .thenReturn(Optional.of(estimate("ORC-2026-0002")));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(storageService.load(branding.getLogoUrl())).thenReturn(Optional.empty());

        var result = pdfService.generatePdf(companyId, estimateId);

        assertThat(result.bytes()).isNotEmpty();
    }

    @Test
    void generatePdf_sanitizesUnexpectedCharactersInFilename() {
        var estimate = estimate("ORC 2026/0001");
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        var result = pdfService.generatePdf(companyId, estimateId);

        assertThat(result.filename()).isEqualTo("orcamento-ORC20260001.pdf");
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        }
    }

    private Estimate estimate(String number) {
        var estimate = new Estimate();
        estimate.setCompanyId(companyId);
        estimate.setCustomerId(UUID.randomUUID());
        estimate.setNumber(number);
        estimate.setTitle("Pintura interior");
        estimate.setStatus(EstimateStatus.DRAFT);
        estimate.setIssueDate(LocalDate.now());
        estimate.setCurrency("EUR");
        estimate.setVatRate(new BigDecimal("23.00"));
        estimate.setUpfrontPercentage(new BigDecimal("50.00"));
        estimate.setCustomerNameSnapshot("Jane Doe");
        ReflectionTestUtils.setField(estimate, "id", estimateId);
        return estimate;
    }

    private Company company() {
        var company = new Company();
        company.setName("Acme Unipessoal Lda");
        company.setEmail("hello@acme.example");
        company.setCountry("PT");
        return company;
    }
}
