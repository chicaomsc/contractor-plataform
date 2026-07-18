package io.chicaodw.platform.estimate;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateShareRequest;
import io.chicaodw.platform.estimate.application.EstimatePdfService;
import io.chicaodw.platform.estimate.application.EstimateShareService;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateShare;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateShareServiceTest {

    @Mock EstimateRepository estimateRepository;
    @Mock EstimateShareRepository estimateShareRepository;
    @Mock CompanyRepository companyRepository;
    @Mock BrandingRepository brandingRepository;
    @Mock SettingsRepository settingsRepository;
    @Mock EstimatePdfService estimatePdfService;

    EstimateShareService service;

    private UUID companyId;
    private UUID estimateId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        estimateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        service = new EstimateShareService(
                estimateRepository, estimateShareRepository, companyRepository, brandingRepository,
                settingsRepository, estimatePdfService);
    }

    // ── createShare ──────────────────────────────────────────────────────────

    @Test
    void createShare_ownedEstimate_returnsRawTokenAndDefaultsTo30DaysExpiry() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId))
                .thenReturn(Optional.empty());

        var response = service.createShare(companyId, userId, estimateId, null);

        assertThat(response.token()).isNotBlank();
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.expiresAt()).isCloseTo(Instant.now().plus(30, ChronoUnit.DAYS), within(5));

        var captor = ArgumentCaptor.forClass(EstimateShare.class);
        verify(estimateShareRepository).save(captor.capture());
        EstimateShare saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(TokenHasher.sha256Hex(response.token()));
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getEstimateId()).isEqualTo(estimateId);
        assertThat(saved.getCreatedByUserId()).isEqualTo(userId);
    }

    @Test
    void createShare_customExpiresInDays_isRespected() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId))
                .thenReturn(Optional.empty());

        var response = service.createShare(companyId, userId, estimateId, new CreateEstimateShareRequest(7));

        assertThat(response.expiresAt()).isCloseTo(Instant.now().plus(7, ChronoUnit.DAYS), within(5));
    }

    @Test
    void createShare_revokesAnyPreviouslyActiveShare() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        var previous = new EstimateShare();
        previous.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId))
                .thenReturn(Optional.of(previous));

        service.createShare(companyId, userId, estimateId, null);

        assertThat(previous.getRevokedAt()).isNotNull();
    }

    @Test
    void createShare_unknownEstimate_throwsResourceNotFoundException() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShare(companyId, userId, estimateId, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(estimateShareRepository, never()).save(any());
    }

    @Test
    void createShare_estimateBelongsToAnotherCompany_throwsResourceNotFoundException() {
        UUID otherCompanyId = UUID.randomUUID();
        when(estimateRepository.findByIdAndCompanyId(estimateId, otherCompanyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShare(otherCompanyId, userId, estimateId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getShareStatus ───────────────────────────────────────────────────────

    @Test
    void getShareStatus_activeShare_returnsActiveWithNoToken() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        var share = shareExpiringIn(30);
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(estimateId, companyId))
                .thenReturn(Optional.of(share));

        var response = service.getShareStatus(companyId, estimateId);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.token()).isNull();
    }

    @Test
    void getShareStatus_expiredShare_returnsExpired() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        var share = shareExpiringIn(-1);
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(estimateId, companyId))
                .thenReturn(Optional.of(share));

        assertThat(service.getShareStatus(companyId, estimateId).status()).isEqualTo("EXPIRED");
    }

    @Test
    void getShareStatus_revokedShare_returnsRevoked() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        var share = shareExpiringIn(30);
        share.setRevokedAt(Instant.now());
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(estimateId, companyId))
                .thenReturn(Optional.of(share));

        assertThat(service.getShareStatus(companyId, estimateId).status()).isEqualTo("REVOKED");
    }

    @Test
    void getShareStatus_noShareEverCreated_throwsResourceNotFoundException() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(estimateId, companyId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShareStatus(companyId, estimateId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── revokeShare ──────────────────────────────────────────────────────────

    @Test
    void revokeShare_activeShare_setsRevokedAt() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        var share = shareExpiringIn(30);
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId))
                .thenReturn(Optional.of(share));

        service.revokeShare(companyId, estimateId);

        assertThat(share.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeShare_noActiveShare_throwsResourceNotFoundException() {
        when(estimateRepository.findByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        when(estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeShare(companyId, estimateId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── public resolution ────────────────────────────────────────────────────

    @Test
    void getPublicView_unknownToken_throwsResourceNotFoundException() {
        when(estimateShareRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicView("unknown-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicView_expiredToken_throwsResourceNotFoundException_withoutDistinguishingFromUnknown() {
        String rawToken = "expired-token";
        var share = shareExpiringIn(-1);
        when(estimateShareRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.getPublicView(rawToken)).isInstanceOf(ResourceNotFoundException.class);
        verify(estimateShareRepository, never()).save(any());
    }

    @Test
    void getPublicView_revokedToken_throwsResourceNotFoundException() {
        String rawToken = "revoked-token";
        var share = shareExpiringIn(30);
        share.setRevokedAt(Instant.now());
        when(estimateShareRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.getPublicView(rawToken)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicView_validToken_recordsAccessAndReturnsSanitizedFields() {
        String rawToken = "valid-token";
        var share = shareExpiringIn(30);
        ReflectionTestUtils.setField(share, "companyId", companyId);
        ReflectionTestUtils.setField(share, "estimateId", estimateId);
        long previousCount = share.getAccessCount();

        when(estimateShareRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))).thenReturn(Optional.of(share));
        when(estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId)).thenReturn(Optional.of(estimate()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        var response = service.getPublicView(rawToken);

        assertThat(response.customer().name()).isEqualTo("Jane Doe");
        assertThat(response.estimate().number()).isEqualTo("ORC-2026-0001");
        assertThat(share.getAccessCount()).isEqualTo(previousCount + 1);
        assertThat(share.getLastAccessAt()).isNotNull();
        verify(estimateShareRepository, times(1)).save(share);
    }

    @Test
    void getPublicPdf_validToken_delegatesToEstimatePdfServiceWithResolvedIds() {
        String rawToken = "pdf-token";
        var share = shareExpiringIn(30);
        ReflectionTestUtils.setField(share, "companyId", companyId);
        ReflectionTestUtils.setField(share, "estimateId", estimateId);
        when(estimateShareRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))).thenReturn(Optional.of(share));
        var pdfResult = new EstimatePdfService.PdfResult(new byte[]{1, 2, 3}, "orcamento-ORC-2026-0001.pdf");
        when(estimatePdfService.generatePdf(companyId, estimateId)).thenReturn(pdfResult);

        var result = service.getPublicPdf(rawToken);

        assertThat(result).isSameAs(pdfResult);
        assertThat(share.getAccessCount()).isEqualTo(1);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static org.assertj.core.data.TemporalUnitOffset within(long seconds) {
        return org.assertj.core.api.Assertions.within(seconds, ChronoUnit.SECONDS);
    }

    private EstimateShare shareExpiringIn(long days) {
        var share = new EstimateShare();
        share.setCompanyId(companyId);
        share.setEstimateId(estimateId);
        share.setExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));
        return share;
    }

    private Estimate estimate() {
        var estimate = new Estimate();
        estimate.setCompanyId(companyId);
        estimate.setCustomerId(UUID.randomUUID());
        estimate.setNumber("ORC-2026-0001");
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
