package io.chicaodw.platform.company.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.company.api.dto.SettingsResponse;
import io.chicaodw.platform.company.api.dto.UpdateSettingsRequest;
import io.chicaodw.platform.company.application.SettingsService;
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
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/me")
    public SettingsResponse getSettings(@AuthenticationPrincipal JwtPrincipal principal) {
        return settingsService.getSettings(principal.companyId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public SettingsResponse updateSettings(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateSettingsRequest request) {
        return settingsService.updateSettings(principal.companyId(), request);
    }
}
