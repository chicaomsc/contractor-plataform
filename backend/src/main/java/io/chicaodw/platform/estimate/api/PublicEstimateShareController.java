package io.chicaodw.platform.estimate.api;

import io.chicaodw.platform.estimate.api.dto.PublicEstimateShareResponse;
import io.chicaodw.platform.estimate.application.EstimateShareService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Unauthenticated endpoints behind a share token — never behind a predictable/sequential
 * path such as {@code /estimate/{id}}. Every response here is scoped to exactly the fields
 * a customer should see: no companyId, no internal IDs, nothing administrative.
 */
@RestController
@RequestMapping("/public/share")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Unauthenticated endpoints for shared estimates")
public class PublicEstimateShareController {

    private final EstimateShareService estimateShareService;

    @GetMapping("/{token}")
    @Operation(
            summary = "View a shared estimate — no authentication required",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Share is valid"),
                    @ApiResponse(responseCode = "404", description = "Token unknown, expired, or revoked")
            }
    )
    public PublicEstimateShareResponse getShared(@PathVariable String token) {
        return estimateShareService.getPublicView(token);
    }

    @GetMapping("/{token}/pdf")
    @Operation(
            summary = "Download the shared estimate's PDF — reuses the same renderer as the admin download",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated", content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Token unknown, expired, or revoked")
            }
    )
    public ResponseEntity<ByteArrayResource> downloadSharedPdf(@PathVariable String token) {
        var result = estimateShareService.getPublicPdf(token);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(result.bytes()));
    }
}
