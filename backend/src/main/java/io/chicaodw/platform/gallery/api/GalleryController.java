package io.chicaodw.platform.gallery.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.gallery.api.dto.FeatureRequest;
import io.chicaodw.platform.gallery.api.dto.GalleryResponse;
import io.chicaodw.platform.gallery.api.dto.ReorderRequest;
import io.chicaodw.platform.gallery.api.dto.UpdateGalleryRequest;
import io.chicaodw.platform.gallery.application.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/gallery")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Gallery", description = "Portfolio gallery — admin endpoints")
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    @Operation(summary = "List all gallery items for the authenticated company")
    public List<GalleryResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return galleryService.listItems(principal.companyId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new gallery item")
    public GalleryResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateGalleryRequest request) {
        return galleryService.createItem(principal.companyId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a gallery item (partial update — null fields are ignored)")
    public GalleryResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGalleryRequest request) {
        return galleryService.updateItem(principal.companyId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a gallery item and its images from storage")
    public void delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        galleryService.deleteItem(principal.companyId(), id);
    }

    @PatchMapping("/{id}/feature")
    @Operation(summary = "Set the featured flag on a gallery item")
    public GalleryResponse feature(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody FeatureRequest request) {
        return galleryService.feature(principal.companyId(), id, request.featured());
    }

    @PatchMapping("/{id}/reorder")
    @Operation(summary = "Set the displayOrder of a gallery item")
    public GalleryResponse reorder(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReorderRequest request) {
        return galleryService.reorder(principal.companyId(), id, request.displayOrder());
    }

    @PostMapping(value = "/{id}/before-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the before image for a gallery item")
    public GalleryResponse uploadBeforeImage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return galleryService.uploadBeforeImage(principal.companyId(), id, file);
    }

    @PostMapping(value = "/{id}/after-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the after image for a gallery item")
    public GalleryResponse uploadAfterImage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return galleryService.uploadAfterImage(principal.companyId(), id, file);
    }

    @DeleteMapping("/{id}/before-image")
    @Operation(summary = "Delete the before image of a gallery item")
    public GalleryResponse deleteBeforeImage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        return galleryService.deleteBeforeImage(principal.companyId(), id);
    }

    @DeleteMapping("/{id}/after-image")
    @Operation(summary = "Delete the after image of a gallery item")
    public GalleryResponse deleteAfterImage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        return galleryService.deleteAfterImage(principal.companyId(), id);
    }
}
