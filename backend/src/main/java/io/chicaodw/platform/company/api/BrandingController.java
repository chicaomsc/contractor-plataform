package io.chicaodw.platform.company.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import io.chicaodw.platform.company.application.BrandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/branding")
@RequiredArgsConstructor
public class BrandingController {

    private final BrandingService brandingService;

    // DT-017A: branding management — OWNER + MANAGER only, MEMBER excluded (matrix in
    // docs/design/DT-017A-roles-authorization-foundation.md). Both read and write are
    // the same rule here (unlike CompanyController), so one annotation per method is
    // enough — no need to split the class differently.
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public BrandingResponse getBranding(@AuthenticationPrincipal JwtPrincipal principal) {
        return brandingService.getBranding(principal.companyId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public BrandingResponse updateBranding(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateBrandingRequest request) {
        return brandingService.updateBranding(principal.companyId(), request);
    }
}
