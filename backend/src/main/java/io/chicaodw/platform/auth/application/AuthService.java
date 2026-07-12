package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.MeResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.api.mapper.AuthMapper;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.auth.infrastructure.security.PlatformUserDetails;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
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

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Email already in use: " + request.email());
        }

        Company company = new Company();
        company.setName(request.companyName());
        company.setSlug(generateUniqueSlug(request.companyName()));
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
        user = userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        RefreshToken refresh = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refresh.getToken(),
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

        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", user.getCompanyId()));

        String accessToken  = jwtService.generateAccessToken(user);
        RefreshToken refresh = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refresh.getToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    public AuthResponse refresh(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessRuleException("Refresh token is invalid or expired"));

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", existing.getUserId()));
        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", user.getCompanyId()));

        String accessToken  = jwtService.generateAccessToken(user);
        RefreshToken newToken = issueRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                newToken.getToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
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

    private RefreshToken issueRefreshToken(UUID userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID() + "-" + UUID.randomUUID());
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtl()));
        return refreshTokenRepository.save(token);
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 -]", "")
                .trim()
                .replaceAll("[ -]+", "-");
        if (base.isBlank()) {
            base = "company";
        }
        String slug = base;
        int n = 1;
        while (companyRepository.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }
}
