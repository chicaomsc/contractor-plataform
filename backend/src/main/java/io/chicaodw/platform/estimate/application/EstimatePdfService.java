package io.chicaodw.platform.estimate.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.estimate.pdf.EstimatePdfDocumentFactory;
import io.chicaodw.platform.estimate.pdf.EstimatePdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates estimate PDF generation. Validates ownership, loads the aggregate and the
 * company/branding/settings needed to render, resolves the logo bytes (if any) through the
 * existing storage abstraction, and delegates document assembly/drawing to the factory and
 * renderer respectively. Contains no drawing code and no financial computation — the
 * Estimate's persisted totals and the customer snapshot are passed through as-is.
 *
 * Generating a PDF is read-only: it never changes the Estimate's status or any other field,
 * regardless of the estimate's current status (DRAFT included — see EstimatePdfDocumentFactory
 * for how DRAFT/CANCELLED are marked visually without touching the aggregate).
 */
@Service
@RequiredArgsConstructor
public class EstimatePdfService {

    private final EstimateRepository estimateRepository;
    private final CompanyRepository companyRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;
    private final StorageService storageService;

    private final EstimatePdfDocumentFactory documentFactory = new EstimatePdfDocumentFactory();
    private final EstimatePdfRenderer renderer = new EstimatePdfRenderer();

    public record PdfResult(byte[] bytes, String filename) {}

    @Transactional(readOnly = true)
    public PdfResult generatePdf(UUID companyId, UUID estimateId) {
        var estimate = estimateRepository.findDetailByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", estimateId));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        Branding branding = brandingRepository.findByCompanyId(companyId).orElse(null);
        Settings settings = settingsRepository.findByCompanyId(companyId).orElse(null);
        byte[] logoBytes = resolveLogoBytes(branding);

        var document = documentFactory.assemble(estimate, company, branding, settings, logoBytes);
        byte[] pdfBytes = renderer.render(document);

        return new PdfResult(pdfBytes, buildFilename(estimate.getNumber()));
    }

    private byte[] resolveLogoBytes(Branding branding) {
        if (branding == null || branding.getLogoUrl() == null) {
            return null;
        }
        return storageService.load(branding.getLogoUrl()).orElse(null);
    }

    private static String buildFilename(String estimateNumber) {
        String sanitized = estimateNumber.replaceAll("[^A-Za-z0-9-_]", "");
        return "orcamento-" + sanitized + ".pdf";
    }
}
