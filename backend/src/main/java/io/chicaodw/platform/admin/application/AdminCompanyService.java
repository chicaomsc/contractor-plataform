package io.chicaodw.platform.admin.application;

import io.chicaodw.platform.admin.api.dto.CompanyAdminDetail;
import io.chicaodw.platform.admin.api.dto.CompanyAdminDetailResponse;
import io.chicaodw.platform.admin.api.dto.CompanyAdminSummary;
import io.chicaodw.platform.admin.api.dto.CompanyOnboardingResponse;
import io.chicaodw.platform.admin.api.dto.CompanySummary;
import io.chicaodw.platform.admin.api.dto.CreateCompanyRequest;
import io.chicaodw.platform.admin.api.dto.InviteOwnerRequest;
import io.chicaodw.platform.admin.api.dto.InviteResponse;
import io.chicaodw.platform.admin.api.dto.OwnerAdminResponse;
import io.chicaodw.platform.admin.api.dto.OwnerInviteResponse;
import io.chicaodw.platform.admin.api.dto.OwnerSummary;
import io.chicaodw.platform.auth.application.InviteService;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserRoleInvariant;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.SecureTokenGenerator;
import io.chicaodw.platform.company.application.CompanySlugGenerator;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SUPER_ADMIN-only orchestration of Company/OWNER onboarding, listing, status
 * management and owner-invite lifecycle (DT-011A.7 §9/§10). Company+Branding+
 * Settings+PENDING-OWNER+invite creation is one atomic {@code @Transactional}
 * operation, mirroring the pattern AuthService.register() already uses for
 * self-service registration — see DT §10.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminCompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final BrandingRepository brandingRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final InviteService inviteService;

    public CompanyOnboardingResponse createCompanyWithOwner(CreateCompanyRequest request, UUID actingSuperAdminId) {
        if (userRepository.existsByEmail(request.ownerEmail())) {
            throw new BusinessRuleException("Email already in use: " + request.ownerEmail());
        }

        String slug = resolveSlug(request);
        if (companyRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Slug already in use: " + slug);
        }

        Company company = new Company();
        company.setName(request.companyName());
        company.setSlug(slug);
        company.setEmail(request.ownerEmail());
        company.setCountry(request.country().toUpperCase(Locale.ROOT));
        if (request.tradeName() != null && !request.tradeName().isBlank()) {
            company.setTradeName(request.tradeName());
        }
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);

        // Same defaults as AuthService.register() — branding/settings configuration
        // is not required at onboarding time (DT §10).
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

        User owner = newPendingOwner(company.getId(), request.ownerEmail(), request.ownerName());
        owner = userRepository.save(owner);

        InviteService.IssuedInvite issued = inviteService.createInvite(owner.getId(), actingSuperAdminId);

        return new CompanyOnboardingResponse(
                toCompanySummary(company),
                toOwnerSummary(owner),
                new InviteResponse(issued.rawToken(), issued.expiresAt())
        );
    }

    @Transactional(readOnly = true)
    public Page<CompanyAdminSummary> listCompanies(String statusParam, String search, Pageable pageable) {
        CompanyStatus status = parseStatus(statusParam);
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Company> page = companyRepository.search(status, normalizedSearch, pageable);

        List<UUID> companyIds = page.getContent().stream().map(Company::getId).toList();
        Map<UUID, String> ownerEmailByCompany = companyIds.isEmpty()
                ? Map.of()
                : userRepository.findByCompanyIdIn(companyIds).stream()
                        .sorted(Comparator.comparing(User::getCreatedAt))
                        .collect(Collectors.toMap(User::getCompanyId, User::getEmail, (first, second) -> first));

        List<CompanyAdminSummary> summaries = page.getContent().stream()
                .map(c -> new CompanyAdminSummary(
                        c.getId(), c.getName(), c.getSlug(), c.getStatus().name(),
                        ownerEmailByCompany.get(c.getId()), c.getCreatedAt()))
                .toList();

        return new PageImpl<>(summaries, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CompanyAdminDetailResponse getCompanyDetail(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        List<OwnerAdminResponse> owners = userRepository.findByCompanyId(companyId).stream()
                .map(this::toOwnerAdminResponse)
                .toList();
        return new CompanyAdminDetailResponse(toCompanyAdminDetail(company), owners);
    }

    public CompanyAdminSummary updateStatus(UUID companyId, String statusValue) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        company.setStatus(parseRequiredStatus(statusValue));
        company = companyRepository.save(company);

        String ownerEmail = userRepository.findByCompanyId(companyId).stream()
                .min(Comparator.comparing(User::getCreatedAt))
                .map(User::getEmail)
                .orElse(null);

        return new CompanyAdminSummary(company.getId(), company.getName(), company.getSlug(),
                company.getStatus().name(), ownerEmail, company.getCreatedAt());
    }

    public OwnerInviteResponse inviteOwner(UUID companyId, InviteOwnerRequest request, UUID actingSuperAdminId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        if (userRepository.existsByEmail(request.ownerEmail())) {
            throw new BusinessRuleException("Email already in use: " + request.ownerEmail());
        }

        User owner = newPendingOwner(company.getId(), request.ownerEmail(), request.ownerName());
        owner = userRepository.save(owner);

        InviteService.IssuedInvite issued = inviteService.createInvite(owner.getId(), actingSuperAdminId);
        return new OwnerInviteResponse(toOwnerSummary(owner), new InviteResponse(issued.rawToken(), issued.expiresAt()));
    }

    public InviteResponse reissueInvite(UUID companyId, UUID ownerId, UUID actingSuperAdminId) {
        requireOwnerInCompany(companyId, ownerId);
        InviteService.IssuedInvite issued = inviteService.reissueInvite(ownerId, actingSuperAdminId);
        return new InviteResponse(issued.rawToken(), issued.expiresAt());
    }

    public void revokeInvite(UUID companyId, UUID ownerId) {
        requireOwnerInCompany(companyId, ownerId);
        inviteService.revokeInvite(ownerId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User newPendingOwner(UUID companyId, String email, String name) {
        User owner = new User();
        owner.setCompanyId(companyId);
        owner.setEmail(email);
        // Never-revealed, never-guessable placeholder — a PENDING user cannot log
        // in regardless of password (PlatformUserDetails.isEnabled() only allows
        // ACTIVE), the real password is set once via InviteService.acceptInvite.
        owner.setPasswordHash(passwordEncoder.encode(SecureTokenGenerator.generate()));
        owner.setName(name);
        owner.setRole(UserRole.OWNER);
        owner.setStatus(UserStatus.PENDING);
        UserRoleInvariant.validate(owner.getRole(), owner.getCompanyId());
        return owner;
    }

    private void requireOwnerInCompany(UUID companyId, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        if (!companyId.equals(owner.getCompanyId())) {
            throw new ResourceNotFoundException("User", ownerId);
        }
    }

    private String resolveSlug(CreateCompanyRequest request) {
        if (request.slug() != null && !request.slug().isBlank()) {
            return request.slug().trim().toLowerCase(Locale.ROOT);
        }
        return CompanySlugGenerator.generateUnique(request.companyName(), companyRepository);
    }

    private CompanyStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CompanyStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid status filter: " + value);
        }
    }

    private CompanyStatus parseRequiredStatus(String value) {
        CompanyStatus status = parseStatus(value);
        if (status == null) {
            throw new BusinessRuleException("Status is required");
        }
        return status;
    }

    private CompanySummary toCompanySummary(Company c) {
        return new CompanySummary(c.getId(), c.getName(), c.getSlug(), c.getCountry(), c.getStatus().name());
    }

    private OwnerSummary toOwnerSummary(User u) {
        return new OwnerSummary(u.getId(), u.getEmail(), u.getName(), u.getStatus().name());
    }

    private CompanyAdminDetail toCompanyAdminDetail(Company c) {
        return new CompanyAdminDetail(c.getId(), c.getName(), c.getSlug(), c.getEmail(), c.getCountry(),
                c.getTradeName(), c.getStatus().name(), c.getCreatedAt());
    }

    private OwnerAdminResponse toOwnerAdminResponse(User u) {
        return new OwnerAdminResponse(u.getId(), u.getEmail(), u.getName(), u.getRole().name(),
                u.getStatus().name(), u.getCreatedAt());
    }
}
