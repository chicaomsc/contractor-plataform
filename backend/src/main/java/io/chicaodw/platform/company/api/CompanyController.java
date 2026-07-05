package io.chicaodw.platform.company.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.company.api.dto.BrandingResponse;
import io.chicaodw.platform.company.api.dto.CompanyResponse;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.company.application.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/me")
    public CompanyResponse getProfile(@AuthenticationPrincipal JwtPrincipal principal) {
        return companyService.getProfile(principal.companyId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public CompanyResponse updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.updateProfile(principal.companyId(), request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse uploadLogo(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return companyService.uploadLogo(principal.companyId(), file);
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(NO_CONTENT)
    public void deleteLogo(@AuthenticationPrincipal JwtPrincipal principal) {
        companyService.deleteLogo(principal.companyId());
    }
}
