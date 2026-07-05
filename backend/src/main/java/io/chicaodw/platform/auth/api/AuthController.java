package io.chicaodw.platform.auth.api;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.MeResponse;
import io.chicaodw.platform.auth.api.dto.RefreshTokenRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.application.AuthService;
import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new contractor account (creates Company + User)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's full profile")
    public MeResponse me(@AuthenticationPrincipal JwtPrincipal principal) {
        return authService.me(principal.userId());
    }
}
