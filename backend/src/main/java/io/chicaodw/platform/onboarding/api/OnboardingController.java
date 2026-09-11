package io.chicaodw.platform.onboarding.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.onboarding.api.dto.OnboardingStatusResponse;
import io.chicaodw.platform.onboarding.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DT-018A — derived onboarding-checklist status for the authenticated OWNER's Company.
 * Every flag is computed live from existing entity state (Company/Branding/Service/
 * Customer/Estimate/TeamInvitation) — nothing is persisted, and there is deliberately no
 * "mark step complete" mutation anywhere in this controller: a step can only ever report
 * done because the underlying data genuinely exists.
 *
 * <p>OWNER-only, same reasoning as {@link io.chicaodw.platform.auth.api.TeamMemberController}
 * / {@link io.chicaodw.platform.auth.api.TeamInvitationController}: MANAGER/MEMBER can
 * perform some of the underlying actions (branding, services, ...) but never the
 * company-profile or team steps (DT-017A matrix — both OWNER-only), so the checklist as
 * a whole is scoped to OWNER. {@code companyId} is never accepted from the client — it
 * comes exclusively from {@link JwtPrincipal}.
 */
@RestController
@RequestMapping("/onboarding")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "OWNER-only: derived onboarding checklist status")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    @Operation(summary = "Get the derived onboarding checklist status for the authenticated company")
    public OnboardingStatusResponse getStatus(@AuthenticationPrincipal JwtPrincipal principal) {
        return onboardingService.getStatus(principal.companyId());
    }
}
