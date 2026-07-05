package io.chicaodw.platform.auth;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.LoginRequest;
import io.chicaodw.platform.auth.api.dto.MeResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.auth.api.mapper.AuthMapper;
import io.chicaodw.platform.auth.application.AuthService;
import io.chicaodw.platform.auth.application.JwtService;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.auth.infrastructure.security.PlatformUserDetails;
import io.chicaodw.platform.auth.api.dto.UserResponse;
import io.chicaodw.platform.auth.api.dto.CompanyResponse;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private BrandingRepository brandingRepository;
    @Mock private SettingsRepository settingsRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthMapper authMapper;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UUID companyId;
    private User user;
    private Company company;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setRefreshTokenTtl(2592000);
        ReflectionTestUtils.setField(authService, "jwtProperties", props);

        userId    = UUID.randomUUID();
        companyId = UUID.randomUUID();

        user = new User();
        ReflectionTestUtils.setField(user, "id",        userId);
        ReflectionTestUtils.setField(user, "companyId", companyId);
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        user.setName("Alice");
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);

        company = new Company();
        ReflectionTestUtils.setField(company, "id", companyId);
        company.setName("Acme");
        company.setSlug("acme");
        company.setEmail("alice@example.com");
        company.setCountry("PT");
        company.setStatus(CompanyStatus.ACTIVE);
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsAuthResponse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(companyRepository.existsBySlug(anyString())).thenReturn(false);
        when(companyRepository.save(any())).thenReturn(company);
        when(brandingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
            RefreshToken rt = inv.getArgument(0);
            ReflectionTestUtils.setField(rt, "id", UUID.randomUUID());
            return rt;
        });
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(userId, companyId, "alice@example.com", "Alice", "OWNER", "ACTIVE"));
        when(authMapper.toCompanyResponse(any())).thenReturn(
                new CompanyResponse(companyId, "Acme", "acme", "alice@example.com", "PT", "ACTIVE"));

        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password1", "Acme", "PT");
        AuthResponse response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.company().slug()).isEqualTo("acme");
    }

    @Test
    void register_duplicateEmail_throwsBusinessRuleException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password1", "Acme", "PT");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("alice@example.com");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsAuthResponse() {
        var userDetails = new PlatformUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
            RefreshToken rt = inv.getArgument(0);
            ReflectionTestUtils.setField(rt, "id", UUID.randomUUID());
            return rt;
        });
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(userId, companyId, "alice@example.com", "Alice", "OWNER", "ACTIVE"));
        when(authMapper.toCompanyResponse(any())).thenReturn(
                new CompanyResponse(companyId, "Acme", "acme", "alice@example.com", "PT", "ACTIVE"));

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password1"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_wrongPassword_propagatesBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        RefreshToken stored = new RefreshToken();
        ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
        stored.setUserId(userId);
        stored.setToken("valid-token");
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(userId, companyId, "alice@example.com", "Alice", "OWNER", "ACTIVE"));
        when(authMapper.toCompanyResponse(any())).thenReturn(
                new CompanyResponse(companyId, "Acme", "acme", "alice@example.com", "PT", "ACTIVE"));

        AuthResponse response = authService.refresh("valid-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refresh_expiredToken_throwsBusinessRuleException() {
        RefreshToken expired = new RefreshToken();
        expired.setToken("expired-token");
        expired.setExpiresAt(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @Test
    void me_authenticated_returnsFullProfile() {
        Branding branding = new Branding();
        ReflectionTestUtils.setField(branding, "id", UUID.randomUUID());
        branding.setCompanyId(companyId);

        Settings settings = new Settings();
        ReflectionTestUtils.setField(settings, "id", UUID.randomUUID());
        settings.setCompanyId(companyId);
        settings.setDefaultCurrency("EUR");
        settings.setDefaultTaxRate(BigDecimal.valueOf(23));
        settings.setEstimateValidityDays(30);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(brandingRepository.findByCompanyId(companyId)).thenReturn(Optional.of(branding));
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(userId, companyId, "alice@example.com", "Alice", "OWNER", "ACTIVE"));
        when(authMapper.toCompanyResponse(any())).thenReturn(
                new CompanyResponse(companyId, "Acme", "acme", "alice@example.com", "PT", "ACTIVE"));
        when(authMapper.toBrandingResponse(any())).thenReturn(null);
        when(authMapper.toSettingsResponse(any())).thenReturn(null);

        MeResponse me = authService.me(userId);

        assertThat(me.user().email()).isEqualTo("alice@example.com");
        assertThat(me.company().name()).isEqualTo("Acme");
    }
}
