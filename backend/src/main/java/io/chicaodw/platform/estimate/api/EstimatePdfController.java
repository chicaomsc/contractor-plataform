package io.chicaodw.platform.estimate.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.estimate.application.EstimatePdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/estimates")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Estimates", description = "Estimate management — admin endpoints. All financial calculations are backend-owned.")
public class EstimatePdfController {

    private final EstimatePdfService estimatePdfService;

    @GetMapping("/{id}/pdf")
    @Operation(
            summary = "Download a commercial-ready PDF of the estimate",
            description = "Renders the estimate's persisted totals, item/material lines and a frozen customer "
                    + "snapshot into a PDF. Available in every estimate status; DRAFT/CANCELLED are marked "
                    + "visually. Never changes the estimate's status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated", content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "401", description = "Not authenticated"),
                    @ApiResponse(responseCode = "404", description = "Estimate not found or belongs to another company")
            }
    )
    public ResponseEntity<ByteArrayResource> downloadPdf(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        var result = estimatePdfService.generatePdf(principal.companyId(), id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(result.bytes()));
    }
}
