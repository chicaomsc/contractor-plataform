package io.chicaodw.platform.estimate.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateResponse;
import io.chicaodw.platform.estimate.api.dto.EstimateSummaryResponse;
import io.chicaodw.platform.estimate.api.dto.UpdateEstimateRequest;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estimates")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Estimates", description = "Estimate management — admin endpoints. All financial calculations are backend-owned.")
public class EstimateController {

    private final EstimateService estimateService;

    @GetMapping
    @Operation(summary = "List estimates for the authenticated company, optionally filtered by status and/or customer")
    public List<EstimateSummaryResponse> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) EstimateStatus status,
            @RequestParam(required = false) UUID customerId) {
        return estimateService.listEstimates(principal.companyId(), status, customerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an estimate with its items and materials")
    public EstimateResponse get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        return estimateService.getEstimate(principal.companyId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new estimate — number, currency, VAT and upfront percentage are generated/snapshotted by the backend")
    public EstimateResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateEstimateRequest request) {
        return estimateService.createEstimate(principal.companyId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an estimate — only allowed while status is DRAFT")
    public EstimateResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstimateRequest request) {
        return estimateService.updateEstimate(principal.companyId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an estimate — only allowed while status is DRAFT; use the status endpoint to cancel otherwise")
    public void delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        estimateService.deleteEstimate(principal.companyId(), id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the estimate status — only whitelisted transitions are allowed")
    public EstimateResponse changeStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeEstimateStatusRequest request) {
        return estimateService.changeStatus(principal.companyId(), id, request);
    }
}
