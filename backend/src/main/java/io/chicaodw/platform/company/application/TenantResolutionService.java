package io.chicaodw.platform.company.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.infrastructure.config.TenantProperties;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Resolves a Host value to a company slug at runtime (DT-011A.7 §16/§17).
 *
 * {@code rawHost} must be the effective {@code Host} header of the actual HTTP request
 * that reached {@link io.chicaodw.platform.company.api.PublicTenantController} — never
 * a client-supplied query parameter, and never {@code X-Forwarded-Host} (deliberately
 * not read anywhere in this path, so a spoofed value has nothing to influence here —
 * see DT §17 for the full trust analysis). This service itself is header-agnostic; the
 * controller is the one place responsible for extracting the correct value.
 *
 * Custom domains (host not ending in the platform's own base domain) are not resolved
 * this sprint — company_domains is deliberately not implemented yet (DT §18).
 */
@Service
@RequiredArgsConstructor
public class TenantResolutionService {

    private final CompanyRepository companyRepository;
    private final TenantProperties tenantProperties;

    @Transactional(readOnly = true)
    public String resolveSlug(String rawHost) {
        String candidate = extractSubdomainSlug(rawHost);
        if (candidate != null && companyRepository.existsBySlug(candidate)) {
            return candidate;
        }

        String fallback = tenantProperties.getDefaultTenantSlug();
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        throw new ResourceNotFoundException("Tenant", rawHost);
    }

    private String extractSubdomainSlug(String rawHost) {
        String baseDomain = tenantProperties.getBaseDomain();
        if (rawHost == null || rawHost.isBlank() || baseDomain == null || baseDomain.isBlank()) {
            return null;
        }

        String host = stripPort(rawHost).toLowerCase(Locale.ROOT);
        String suffix = "." + baseDomain.toLowerCase(Locale.ROOT);
        if (!host.endsWith(suffix)) {
            return null;
        }

        String candidate = host.substring(0, host.length() - suffix.length());
        // Only a single label is supported this sprint — a dotted remainder means
        // something like "a.b.<baseDomain>", not a plain "<slug>.<baseDomain>".
        if (candidate.isBlank() || candidate.contains(".")) {
            return null;
        }
        return candidate;
    }

    private String stripPort(String host) {
        int idx = host.indexOf(':');
        return idx >= 0 ? host.substring(0, idx) : host;
    }
}
