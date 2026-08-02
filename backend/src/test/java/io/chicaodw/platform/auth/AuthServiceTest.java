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
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.security.TokenHasher;
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
import org.springframework.security.authentication.DisabledException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    void register_duplicateEmail_throwsConflictWithoutEchoingTheEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password1", "Acme", "PT");

        // SEC-AUTH-05/Sprint 11B.6D: 409 (a real conflict, not a generic 422 business
        // rule), and the message no longer echoes the submitted email back.
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageNotContaining("alice@example.com");
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

    @Test
    void login_inactiveCompany_throwsDisabledException() {
        company.setStatus(CompanyStatus.INACTIVE);
        var userDetails = new PlatformUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "password1")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void login_superAdmin_returnsNullCompany_noNpe() {
        User superAdmin = new User();
        ReflectionTestUtils.setField(superAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(superAdmin, "companyId", null);
        superAdmin.setEmail("admin@example.com");
        superAdmin.setPasswordHash("hashed");
        superAdmin.setName("Platform Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);

        var userDetails = new PlatformUserDetails(superAdmin);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
            RefreshToken rt = inv.getArgument(0);
            ReflectionTestUtils.setField(rt, "id", UUID.randomUUID());
            return rt;
        });
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(superAdmin.getId(), null, "admin@example.com", "Platform Admin", "SUPER_ADMIN", "ACTIVE"));

        AuthResponse response = authService.login(new LoginRequest("admin@example.com", "password1"));

        assertThat(response.company()).isNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        RefreshToken stored = new RefreshToken();
        ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
        stored.setUserId(userId);
        stored.setTokenHash(TokenHasher.sha256Hex("valid-token"));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex("valid-token"))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.markRevokedIfStillValid(eq(stored.getId()), any())).thenReturn(1);
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
        verify(refreshTokenRepository).markRevokedIfStillValid(eq(stored.getId()), any());
    }

    @Test
    void refresh_expiredOrAlreadyRevokedToken_throwsBusinessRuleException() {
        RefreshToken expired = new RefreshToken();
        ReflectionTestUtils.setField(expired, "id", UUID.randomUUID());
        expired.setTokenHash(TokenHasher.sha256Hex("expired-token"));
        expired.setExpiresAt(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex("expired-token"))).thenReturn(Optional.of(expired));
        // The atomic UPDATE's own WHERE clause (revoked = false AND expires_at > now)
        // is what actually enforces this in production — a mock can only simulate its
        // result, not its SQL, so this stub is what makes "expired" observable here.
        when(refreshTokenRepository.markRevokedIfStillValid(eq(expired.getId()), any())).thenReturn(0);

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_unknownToken_throwsBusinessRuleException() {
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex("unknown-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_inactiveCompany_throwsDisabledException() {
        RefreshToken stored = new RefreshToken();
        ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
        stored.setUserId(userId);
        stored.setTokenHash(TokenHasher.sha256Hex("valid-token"));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        company.setStatus(CompanyStatus.INACTIVE);
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex("valid-token"))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.markRevokedIfStillValid(eq(stored.getId()), any())).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> authService.refresh("valid-token"))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void refresh_inactiveUser_throwsDisabledException() {
        RefreshToken stored = new RefreshToken();
        ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
        stored.setUserId(userId);
        stored.setTokenHash(TokenHasher.sha256Hex("valid-token"));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        user.setStatus(UserStatus.INACTIVE);
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex("valid-token"))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.markRevokedIfStillValid(eq(stored.getId()), any())).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh("valid-token"))
                .isInstanceOf(DisabledException.class);
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @Test
    void me_superAdmin_returnsNullCompanyBrandingAndSettings() {
        User superAdmin = new User();
        UUID superAdminId = UUID.randomUUID();
        ReflectionTestUtils.setField(superAdmin, "id", superAdminId);
        ReflectionTestUtils.setField(superAdmin, "companyId", null);
        superAdmin.setEmail("admin@example.com");
        superAdmin.setPasswordHash("hashed");
        superAdmin.setName("Platform Admin");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(superAdminId)).thenReturn(Optional.of(superAdmin));
        when(authMapper.toUserResponse(any())).thenReturn(
                new UserResponse(superAdminId, null, "admin@example.com", "Platform Admin", "SUPER_ADMIN", "ACTIVE"));

        MeResponse me = authService.me(superAdminId);

        assertThat(me.company()).isNull();
        assertThat(me.branding()).isNull();
        assertThat(me.settings()).isNull();
    }

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
