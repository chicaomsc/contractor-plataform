package io.chicaodw.platform.estimate.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateShareRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateShareResponse;
import io.chicaodw.platform.estimate.application.EstimateShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/estimates")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Estimates", description = "Estimate management — admin endpoints. All financial calculations are backend-owned.")
public class EstimateShareController {

    private final EstimateShareService estimateShareService;

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create (or regenerate) a public share link for the estimate",
            description = "Revokes any previously active link for this estimate. The raw token/URL is only "
                    + "ever returned in this response — only its hash is stored, so it cannot be retrieved again."
    )
    public EstimateShareResponse createShare(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CreateEstimateShareRequest request) {
        return estimateShareService.createShare(principal.companyId(), principal.userId(), id, request);
    }

    @GetMapping("/{id}/share")
    @Operation(summary = "Get the current share link status (no raw token — only its hash is stored)")
    public EstimateShareResponse getShare(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        return estimateShareService.getShareStatus(principal.companyId(), id);
    }

    @DeleteMapping("/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the active share link, if any")
    public void revokeShare(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        estimateShareService.revokeShare(principal.companyId(), id);
    }
}
