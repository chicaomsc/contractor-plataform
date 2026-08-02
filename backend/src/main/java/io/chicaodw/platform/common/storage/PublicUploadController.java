package io.chicaodw.platform.common.storage;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Serves previously uploaded company assets (logo, gallery before/after photos).
 * Replaces the plain Spring resource handler that used to serve {@code /uploads/**}
 * unconditionally regardless of company status.
 *
 * Public only while the owning company is ACTIVE — a deactivated company's files return
 * the same 404 as a file that never existed, never revealing which case applies
 * (DT-011B.5 §9, SEC-TENANT-03). The URL shape ({@code /uploads/company/{companyId}/...})
 * is unchanged from before — every stored URL already embeds {@code companyId} (see
 * {@link LocalStorageService#store}), so no data migration is needed; only how the URL
 * is served changes.
 */
@RestController
@RequestMapping("/uploads/company/{companyId}")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Serves uploaded company assets — public only while the company is ACTIVE")
public class PublicUploadController {

    private final StorageService storageService;
    private final CompanyRepository companyRepository;

    @GetMapping("/**")
    @Operation(summary = "Download a previously uploaded company asset (logo, gallery image)")
    public ResponseEntity<byte[]> serve(@PathVariable String companyId, HttpServletRequest request) {
        CompanyStatus status = companyRepository.findStatusById(parseCompanyId(companyId)).orElse(null);
        if (status != CompanyStatus.ACTIVE) {
            throw notFound();
        }

        String storedPath = request.getRequestURI();
        byte[] bytes = storageService.load(storedPath).orElseThrow(PublicUploadController::notFound);

        // no-cache (not no-store): the response is still cacheable, but a client must
        // revalidate before reusing it — and since no ETag/Last-Modified is set, there's
        // nothing to revalidate against, so in practice this forces a fresh request every
        // time. That's deliberate: a 30-day public max-age (the pre-11B.6B behavior) would
        // let a browser keep serving a deactivated company's logo/gallery from its own
        // cache long after CompanyStatus flips to INACTIVE, regardless of what the server
        // does. Content-addressed/versioned URLs (§5, Sprint 11B.6C) would let us go back
        // to a long max-age safely later, since the URL itself would change on replace.
        return ResponseEntity.ok()
                .contentType(resolveMediaType(storedPath))
                .cacheControl(CacheControl.noCache().cachePublic())
                .body(bytes);
    }

    private static UUID parseCompanyId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw notFound();
        }
    }

    private static MediaType resolveMediaType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Upload", "asset");
    }
}
