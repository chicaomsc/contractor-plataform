package io.chicaodw.platform.company.api;

import io.chicaodw.platform.company.api.dto.PublicSiteResponse;
import io.chicaodw.platform.company.application.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/sites")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Unauthenticated endpoints for public sites")
public class PublicSiteController {

    private final CompanyService companyService;

    @GetMapping("/{companySlug}")
    @Operation(summary = "Get public site data for a company by slug")
    public PublicSiteResponse getPublicSite(@PathVariable String companySlug) {
        return companyService.getPublicSite(companySlug);
    }
}
