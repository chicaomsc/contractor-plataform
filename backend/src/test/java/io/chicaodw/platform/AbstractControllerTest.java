package io.chicaodw.platform;

import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.application.BrandingService;
import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.company.application.SettingsService;
import io.chicaodw.platform.gallery.application.GalleryService;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

/**
 * Shared base for all controller tests.
 * Declares all @MockitoBean stubs here so Spring reuses a single application context
 * across all controller test classes.
 */
@AutoConfigureMockMvc
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

    @MockitoBean public CompanyService        companyService;
    @MockitoBean public BrandingService       brandingService;
    @MockitoBean public SettingsService       settingsService;
    @MockitoBean public StorageService        storageService;
    @MockitoBean public ServiceCatalogService catalogService;
    @MockitoBean public GalleryService        galleryService;

    protected static final UUID COMPANY_ID = UUID.randomUUID();
    protected static final UUID USER_ID    = UUID.randomUUID();

    protected static Authentication ownerAuth() {
        var principal = new JwtPrincipal(USER_ID, COMPANY_ID, "owner@test.com", UserRole.OWNER);
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    protected static Authentication nonOwnerAuth() {
        var principal = new JwtPrincipal(USER_ID, COMPANY_ID, "user@test.com", UserRole.OWNER);
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }
}
