package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.MeResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.api.mapper.AuthMapper;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserRoleInvariant;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.auth.infrastructure.security.PlatformUserDetails;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.application.CompanySlugGenerator;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;
    private final JwtProperties jwtProperties;

    // ── Register ─────────────────────────────────────────────────────────────

    /**
     * SEC-AUTH-05/DT-011B.2 §13: a duplicate email still returns a distinct signal
     * (409, not a generic response) — accepted as-is for this B2B self-serve flow
     * (the registrant already knows their own company's email; there is no email
     * provider yet to instead confirm registration out-of-band, see DT-011A.10 §16/§22).
     * The one change from before is that the response no longer echoes the submitted
     * email back in the error message — the client already has it, so repeating it
     * server-side only adds it to logs/error-tracking for no benefit.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        Company company = new Company();
        company.setName(request.companyName());
        company.setSlug(CompanySlugGenerator.generateUnique(request.companyName(), companyRepository));
        company.setEmail(request.email());
        company.setCountry(request.country().toUpperCase(Locale.ROOT));
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);

        Branding branding = new Branding();
        branding.setCompanyId(company.getId());
        branding.setPrimaryColor("#1E40AF");
        branding.setSecondaryColor("#3B82F6");
        branding.setAccentColor("#F59E0B");
        brandingRepository.save(branding);

        Settings settings = new Settings();
        settings.setCompanyId(company.getId());
        settings.setDefaultCurrency("EUR");
        settings.setDefaultTaxRate(new BigDecimal("23.00"));
        settings.setEstimateValidityDays(30);
        settings.setLocale("pt-PT");
        settings.setTimezone("Europe/Lisbon");
        settingsRepository.save(settings);

        User user = new User();
        user.setCompanyId(company.getId());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.ownerName());
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);
        UserRoleInvariant.validate(user.getRole(), user.getCompanyId());
        user = userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        IssuedRefreshToken refresh = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refresh.rawToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = ((PlatformUserDetails) authentication.getPrincipal()).user();

        // PlatformUserDetails.isEnabled() already made Spring Security reject a
        // non-ACTIVE User before we ever get here — but a SUPER_ADMIN has no
        // company, and an OWNER's company may itself be inactive even though the
        // user account is fine, which isEnabled() cannot see.
        Company company = loadCompanyAndAssertActive(user);

        String accessToken  = jwtService.generateAccessToken(user);
        IssuedRefreshToken refresh = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refresh.rawToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * Rotation strategy: option B (rotate on every use, revoke the one just consumed)
     * — already the design before this sprint; the fix here is closing the race
     * between two concurrent calls with the same token (SEC-AUTH-14, Sprint 11B.6D)
     * via an atomic conditional UPDATE, same pattern as {@code
     * PasswordResetTokenService}/{@code InviteService.acceptInvite}. At most one
     * concurrent caller ever observes {@code markRevokedIfStillValid} return 1.
     */
    public AuthResponse refresh(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(tokenValue))
                .orElseThrow(() -> new BusinessRuleException("Refresh token is invalid or expired"));

        int updated = refreshTokenRepository.markRevokedIfStillValid(existing.getId(), Instant.now());
        if (updated == 0) {
            throw new BusinessRuleException("Refresh token is invalid or expired");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", existing.getUserId()));
        // Refresh never goes through AuthenticationManager/PlatformUserDetails, so
        // the User.status check that login() gets "for free" must be explicit here.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Your account is inactive");
        }
        Company company = loadCompanyAndAssertActive(user);

        String accessToken  = jwtService.generateAccessToken(user);
        IssuedRefreshToken newToken = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                newToken.rawToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Revokes the given refresh token so it can never be used again via /auth/refresh.
     * Idempotent by design — a token that's already revoked, unknown, or belongs to a
     * different user is silently a no-op (no error, no signal either way), matching
     * the anti-enumeration posture already used elsewhere in this module. The access
     * token used to authenticate this call is deliberately left untouched — it stays
     * valid until its own natural expiry (DT-011B.5 §9 HARD-02).
     */
    public void logout(UUID userId, String refreshTokenValue) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshTokenValue))
                .filter(t -> t.getUserId().equals(userId))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // SUPER_ADMIN has no company — company/branding/settings are simply absent
        // from the response rather than attempting (and failing) to load them.
        if (user.getCompanyId() == null) {
            return new MeResponse(authMapper.toUserResponse(user), null, null, null);
        }

        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", user.getCompanyId()));
        Branding branding = brandingRepository.findByCompanyId(company.getId()).orElse(null);
        Settings settings = settingsRepository.findByCompanyId(company.getId()).orElse(null);

        return new MeResponse(
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company),
                branding != null ? authMapper.toBrandingResponse(branding) : null,
                settings != null ? authMapper.toSettingsResponse(settings) : null
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads the user's company and enforces it is ACTIVE — null for a SUPER_ADMIN,
     * who has no company. Login/refresh are the only two authenticated flows that
     * don't pass through ActiveAccountFilter (no JwtPrincipal exists yet at that
     * point), so this check is not redundant with the filter — see DT-011A.7 §13.
     */
    private Company loadCompanyAndAssertActive(User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return null;
        }
        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", user.getCompanyId()));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new DisabledException("Your account is inactive");
        }
        return company;
    }

    /** Mirrors InviteService.IssuedInvite/PasswordResetTokenService.IssuedToken — the raw
     * value is only ever available here, at issuance; only its hash is persisted. */
    private record IssuedRefreshToken(String rawToken, Instant expiresAt) {}

    private IssuedRefreshToken issueRefreshToken(UUID userId) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHasher.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtl()));
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }
}
