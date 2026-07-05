package io.chicaodw.platform.servicecatalog.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.dto.ReorderRequest;
import io.chicaodw.platform.servicecatalog.api.dto.ServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.UpdateServiceRequest;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Company service catalogue — admin endpoints")
public class ServiceController {

    private final ServiceCatalogService catalogService;

    @GetMapping
    @Operation(summary = "List all catalogue services for the authenticated company")
    public List<ServiceResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return catalogService.listServices(principal.companyId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new catalogue service")
    public ServiceResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateServiceRequest request) {
        return catalogService.createService(principal.companyId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalogue service (partial update — null fields are ignored)")
    public ServiceResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return catalogService.updateService(principal.companyId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a catalogue service")
    public void delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        catalogService.deleteService(principal.companyId(), id);
    }

    @PatchMapping("/{id}/reorder")
    @Operation(summary = "Set the displayOrder of a catalogue service")
    public ServiceResponse reorder(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReorderRequest request) {
        return catalogService.reorder(principal.companyId(), id, request.displayOrder());
    }
}
