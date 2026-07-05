package io.chicaodw.platform.servicecatalog.api;

import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.servicecatalog.api.dto.PublicServiceResponse;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Unauthenticated endpoints for the landing page")
public class PublicServiceController {

    private final CompanyService       companyService;
    private final ServiceCatalogService catalogService;

    @GetMapping("/services")
    @Operation(summary = "List active services for a company (by slug) — no authentication required")
    public List<PublicServiceResponse> listServices(@RequestParam String slug) {
        var company = companyService.findBySlug(slug);
        return catalogService.listPublicServices(company.getId());
    }
}
