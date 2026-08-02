package io.chicaodw.platform.admin.api;

import io.chicaodw.platform.common.storage.StorageOrphanReport;
import io.chicaodw.platform.common.storage.StorageReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-administration endpoint for storage housekeeping — SUPER_ADMIN only, same
 * defense-in-depth as {@link AdminController} ({@code @PreAuthorize} here, plus
 * SecurityConfig's {@code /admin/**} matcher, plus the per-request
 * {@code ActiveAccountFilter} check).
 *
 * Report-only by design this sprint (Sprint 11B.6C item 4) — there is deliberately no
 * delete endpoint here yet; see {@code docs/operations/runbook.md} for the manual
 * cleanup procedure once an operator has reviewed a report.
 */
@RestController
@RequestMapping("/admin/storage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform Admin", description = "SUPER_ADMIN-only storage housekeeping endpoints")
public class StorageAdminController {

    private final StorageReconciliationService storageReconciliationService;

    @GetMapping("/orphans")
    @Operation(summary = "Report storage keys present on disk but referenced by no branding/gallery row (dry-run only, deletes nothing)")
    public StorageOrphanReport listOrphans() {
        return storageReconciliationService.findOrphans();
    }
}
