package io.chicaodw.platform.company.api;

import io.chicaodw.platform.company.api.dto.TenantResolutionResponse;
import io.chicaodw.platform.company.application.TenantResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Unauthenticated endpoints for public sites")
public class PublicTenantController {

    private final TenantResolutionService tenantResolutionService;

    /**
     * Resolves the effective Host of this very request to a company slug — never a
     * client-supplied query parameter, never X-Forwarded-Host (DT-011A.7 §16/§17,
     * corrected per the approved architecture: the caller cannot hand the backend an
     * arbitrary host value, it can only be resolved from the connection that actually
     * reached this endpoint).
     */
    @GetMapping("/tenant")
    @Operation(summary = "Resolve the effective Host of this request to a company slug")
    public TenantResolutionResponse resolveTenant(HttpServletRequest request) {
        String host = request.getHeader(HttpHeaders.HOST);
        return new TenantResolutionResponse(tenantResolutionService.resolveSlug(host));
    }
}
