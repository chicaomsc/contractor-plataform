package io.chicaodw.platform;

import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserActiveState;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.application.BrandingService;
import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.company.application.SettingsService;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.gallery.application.GalleryService;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Shared base for all controller tests.
 * Declares all @MockitoBean stubs here so Spring reuses a single application context
 * across all controller test classes.
 *
 * UserRepository/CompanyRepository are mocked here too (nothing in this slice needs
 * real persistence for them — every domain service is already mocked above) purely so
 * ActiveAccountFilter's per-request status check (DT-011A.7 §13/§14) has something to
 * find for the fake USER_ID/COMPANY_ID these tests authenticate as: real controller
 * tests use MockMvc's `authentication(...)` post-processor to inject a JwtPrincipal
 * directly into the SecurityContext, bypassing JwtAuthenticationFilter entirely — but
 * ActiveAccountFilter still runs for real and, without this stub, found no matching
 * User/Company row and correctly rejected every request as inactive. Tests that
 * specifically exercise disabled-account behavior (see ActiveAccountFilterTest,
 * AdminCompanyStatusTest) use real persisted rows instead of this base class, so they
 * are unaffected by this default-active stub.
 */
@AutoConfigureMockMvc
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

    @MockitoBean public CompanyService        companyService;
    @MockitoBean public BrandingService       brandingService;
    @MockitoBean public SettingsService       settingsService;
    @MockitoBean public StorageService        storageService;
    @MockitoBean public ServiceCatalogService catalogService;
    @MockitoBean public GalleryService        galleryService;
    @MockitoBean public CustomerService       customerService;
    @MockitoBean public EstimateService       estimateService;
    @MockitoBean public UserRepository        userRepository;
    @MockitoBean public CompanyRepository     companyRepository;

    protected static final UUID COMPANY_ID = UUID.randomUUID();
    protected static final UUID USER_ID    = UUID.randomUUID();

    @BeforeEach
    void stubDefaultActiveAccount() {
        when(userRepository.findActiveStateById(eq(USER_ID)))
                .thenReturn(Optional.of(new UserActiveState(UserStatus.ACTIVE, 0L)));
        when(companyRepository.findStatusById(eq(COMPANY_ID))).thenReturn(Optional.of(CompanyStatus.ACTIVE));
    }

    protected static Authentication ownerAuth() {
        var principal = new JwtPrincipal(USER_ID, COMPANY_ID, "owner@test.com", UserRole.OWNER, 0L);
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    protected static Authentication nonOwnerAuth() {
        var principal = new JwtPrincipal(USER_ID, COMPANY_ID, "user@test.com", UserRole.OWNER, 0L);
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }

    /** companyId is deliberately null, exactly like a real SUPER_ADMIN principal (DT-011B.6B, SEC-TENANT-05). */
    protected static Authentication superAdminAuth() {
        var principal = new JwtPrincipal(USER_ID, null, "super-admin@test.com", UserRole.SUPER_ADMIN, 0L);
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }
}
