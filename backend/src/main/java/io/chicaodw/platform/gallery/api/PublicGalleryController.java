package io.chicaodw.platform.gallery.api;

import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.gallery.api.dto.PublicGalleryResponse;
import io.chicaodw.platform.gallery.application.GalleryService;
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
public class PublicGalleryController {

    private final CompanyService companyService;
    private final GalleryService galleryService;

    @GetMapping("/gallery")
    @Operation(summary = "List active gallery items for a company (by slug) — featured first, no authentication required")
    public List<PublicGalleryResponse> listGallery(@RequestParam String slug) {
        var company = companyService.findBySlug(slug);
        return galleryService.listPublicItems(company.getId());
    }
}
