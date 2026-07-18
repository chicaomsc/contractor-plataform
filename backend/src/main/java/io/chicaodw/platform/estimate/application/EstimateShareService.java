package io.chicaodw.platform.estimate.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.SecureTokenGenerator;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateShareRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateShareResponse;
import io.chicaodw.platform.estimate.api.dto.PublicEstimateShareResponse;
import io.chicaodw.platform.estimate.domain.EstimateShare;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateShareRepository;
import io.chicaodw.platform.estimate.pdf.EstimatePdfDocument;
import io.chicaodw.platform.estimate.pdf.EstimatePdfDocumentFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns the full lifecycle of estimate share links: creation (owner-only), status lookup,
 * revocation, and the token-authenticated public resolution used by the unauthenticated
 * {@code /public/share/**} endpoints.
 *
 * Security notes:
 * <ul>
 *   <li>Only {@link EstimateShare#getTokenHash()} (SHA-256) is persisted — the raw token is
 *       returned to the owner exactly once, in the {@link #createShare} response, and is not
 *       recoverable afterwards.</li>
 *   <li>Token resolution treats "token doesn't exist", "expired", and "revoked" all as the
 *       same 404 — never revealing which case applies, so an attacker probing tokens gets no
 *       signal.</li>
 *   <li>Creating a new share revokes any previously active one for the same estimate: at most
 *       one usable link exists per estimate at a time.</li>
 * </ul>
 *
 * The PDF download is not generated here: the resolved (companyId, estimateId) pair is handed
 * to the existing {@link EstimatePdfService}, so the public
 * download reuses the exact same renderer as the authenticated one. The public JSON view is
 * built from the same {@link EstimatePdfDocumentFactory} output as the PDF, so both surfaces
 * stay consistent without duplicating any formatting/financial logic.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EstimateShareService {

    private static final int DEFAULT_EXPIRES_IN_DAYS = 30;

    private final EstimateRepository estimateRepository;
    private final EstimateShareRepository estimateShareRepository;
    private final CompanyRepository companyRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;
    private final EstimatePdfService estimatePdfService;

    private final EstimatePdfDocumentFactory documentFactory = new EstimatePdfDocumentFactory();

    // ── Private (owner-only) API ────────────────────────────────────────────

    public EstimateShareResponse createShare(UUID companyId, UUID userId, UUID estimateId, CreateEstimateShareRequest request) {
        requireOwnedEstimate(companyId, estimateId);

        estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId)
                .ifPresent(existing -> existing.setRevokedAt(Instant.now()));

        String rawToken = SecureTokenGenerator.generate();

        var share = new EstimateShare();
        share.setCompanyId(companyId);
        share.setEstimateId(estimateId);
        share.setTokenHash(TokenHasher.sha256Hex(rawToken));
        share.setExpiresAt(Instant.now().plus(resolveExpiresInDays(request), java.time.temporal.ChronoUnit.DAYS));
        share.setCreatedByUserId(userId);

        estimateShareRepository.save(share);
        return toResponse(share, rawToken);
    }

    @Transactional(readOnly = true)
    public EstimateShareResponse getShareStatus(UUID companyId, UUID estimateId) {
        requireOwnedEstimate(companyId, estimateId);
        var share = estimateShareRepository.findFirstByEstimateIdAndCompanyIdOrderByCreatedAtDesc(estimateId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("EstimateShare", estimateId));
        return toResponse(share, null);
    }

    public void revokeShare(UUID companyId, UUID estimateId) {
        requireOwnedEstimate(companyId, estimateId);
        var share = estimateShareRepository.findFirstByEstimateIdAndCompanyIdAndRevokedAtIsNull(estimateId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("EstimateShare", estimateId));
        share.setRevokedAt(Instant.now());
    }

    private void requireOwnedEstimate(UUID companyId, UUID estimateId) {
        estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", estimateId));
    }

    private int resolveExpiresInDays(CreateEstimateShareRequest request) {
        Integer requested = request != null ? request.expiresInDays() : null;
        return requested != null ? requested : DEFAULT_EXPIRES_IN_DAYS;
    }

    private EstimateShareResponse toResponse(EstimateShare share, String rawToken) {
        return new EstimateShareResponse(
                share.getId(),
                computeStatus(share),
                rawToken,
                share.getCreatedAt(),
                share.getExpiresAt(),
                share.getRevokedAt(),
                share.getLastAccessAt(),
                share.getAccessCount()
        );
    }

    private String computeStatus(EstimateShare share) {
        if (share.isRevoked()) return "REVOKED";
        if (share.isExpired()) return "EXPIRED";
        return "ACTIVE";
    }

    // ── Public (unauthenticated, token-scoped) API ──────────────────────────

    public PublicEstimateShareResponse getPublicView(String rawToken) {
        var share = resolveValidShareReadOnly(rawToken);
        recordAccess(share);

        var estimate = estimateRepository.findDetailByIdAndCompanyId(share.getEstimateId(), share.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("EstimateShare", "token"));
        Company company = companyRepository.findById(share.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("EstimateShare", "token"));
        Branding branding = brandingRepository.findByCompanyId(share.getCompanyId()).orElse(null);
        Settings settings = settingsRepository.findByCompanyId(share.getCompanyId()).orElse(null);

        var document = documentFactory.assemble(estimate, company, branding, settings, null);
        return toPublicResponse(document, branding);
    }

    public EstimatePdfService.PdfResult getPublicPdf(String rawToken) {
        var share = resolveValidShareReadOnly(rawToken);
        recordAccess(share);
        return estimatePdfService.generatePdf(share.getCompanyId(), share.getEstimateId());
    }

    /**
     * Looks up and validates the token but does not mutate the entity — call sites record the
     * access themselves via {@link #recordAccess} once they've decided the request will
     * actually succeed.
     */
    private EstimateShare resolveValidShareReadOnly(String rawToken) {
        String hash = TokenHasher.sha256Hex(rawToken);
        var share = estimateShareRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("EstimateShare", "token"));
        if (!share.isUsable()) {
            throw new ResourceNotFoundException("EstimateShare", "token");
        }
        return share;
    }

    private void recordAccess(EstimateShare share) {
        share.recordAccess();
        estimateShareRepository.save(share);
    }

    private PublicEstimateShareResponse toPublicResponse(EstimatePdfDocument document, Branding branding) {
        var seller = document.seller();
        var metadata = document.metadata();
        var summary = document.summary();

        return new PublicEstimateShareResponse(
                new PublicEstimateShareResponse.Seller(
                        seller.displayName(),
                        branding != null ? branding.getLogoUrl() : null,
                        seller.phone(),
                        seller.email(),
                        seller.website(),
                        seller.addressLine()
                ),
                new PublicEstimateShareResponse.EstimateInfo(
                        metadata.number(),
                        metadata.title(),
                        metadata.description(),
                        metadata.statusLabel(),
                        document.draft(),
                        document.cancelled(),
                        metadata.issueDateLabel(),
                        metadata.validUntilLabel()
                ),
                new PublicEstimateShareResponse.Customer(document.customer().name()),
                document.items().stream().map(this::toPublicLineItem).toList(),
                document.materials().stream().map(this::toPublicLineItem).toList(),
                new PublicEstimateShareResponse.FinancialSummary(
                        summary.currency(),
                        summary.laborSubtotalLabel(),
                        summary.materialSubtotalLabel(),
                        summary.subtotalLabel(),
                        summary.vatLabel(),
                        summary.vatAmountLabel(),
                        summary.totalLabel(),
                        summary.upfrontLabel(),
                        summary.upfrontAmountLabel(),
                        summary.remainingLabel()
                ),
                document.notes(),
                document.terms()
        );
    }

    private PublicEstimateShareResponse.LineItem toPublicLineItem(EstimatePdfDocument.LineItem item) {
        return new PublicEstimateShareResponse.LineItem(
                item.description(), item.quantityLabel(), item.unitLabel(), item.unitPriceLabel(), item.totalLabel());
    }
}
