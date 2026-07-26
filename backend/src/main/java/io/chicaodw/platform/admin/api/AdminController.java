package io.chicaodw.platform.admin.api;

import io.chicaodw.platform.admin.api.dto.CompanyAdminDetailResponse;
import io.chicaodw.platform.admin.api.dto.CompanyAdminSummary;
import io.chicaodw.platform.admin.api.dto.CompanyOnboardingResponse;
import io.chicaodw.platform.admin.api.dto.CreateCompanyRequest;
import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.InviteResponse;
import io.chicaodw.platform.admin.api.dto.OwnerInviteResponse;
import io.chicaodw.platform.admin.api.dto.UpdateCompanyStatusRequest;
import io.chicaodw.platform.admin.application.AdminCompanyService;
import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform-administration endpoints — SUPER_ADMIN only (DT-011A.7 §6/§9). Enforced
 * both here (method security) and in SecurityConfig's filter-chain matcher, plus the
 * per-request ActiveAccountFilter check — see the DT for the full defense-in-depth
 * rationale.
 */
@RestController
@RequestMapping("/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform Admin", description = "SUPER_ADMIN-only endpoints for provisioning and managing companies")
public class AdminController {

    private final AdminCompanyService adminCompanyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Company and its PENDING OWNER in a single transaction")
    public CompanyOnboardingResponse createCompany(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCompanyRequest request) {
        return adminCompanyService.createCompanyWithOwner(request, principal.userId());
    }

    @GetMapping
    @Operation(summary = "List companies (paginated, optional status/search filters)")
    public Page<CompanyAdminSummary> listCompanies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return adminCompanyService.listCompanies(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company detail plus its owners")
    public CompanyAdminDetailResponse getCompany(@PathVariable UUID id) {
        return adminCompanyService.getCompanyDetail(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a company")
    public CompanyAdminSummary updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyStatusRequest request) {
        return adminCompanyService.updateStatus(id, request.status());
    }

    @PostMapping("/{companyId}/owners")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a new OWNER for an existing company")
    public OwnerInviteResponse inviteOwner(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId,
            @Valid @RequestBody InviteOwnerRequest request) {
        return adminCompanyService.inviteOwner(companyId, request, principal.userId());
    }

    @PostMapping("/{companyId}/owners/{ownerId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reissue an invite for a still-PENDING owner (revokes the previous one)")
    public InviteResponse reissueInvite(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId,
            @PathVariable UUID ownerId) {
        return adminCompanyService.reissueInvite(companyId, ownerId, principal.userId());
    }

    @DeleteMapping("/{companyId}/owners/{ownerId}/invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a still-PENDING owner's invite without reissuing")
    public void revokeInvite(@PathVariable UUID companyId, @PathVariable UUID ownerId) {
        adminCompanyService.revokeInvite(companyId, ownerId);
    }
}
