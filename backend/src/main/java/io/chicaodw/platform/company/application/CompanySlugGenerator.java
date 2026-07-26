package io.chicaodw.platform.company.application;

import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Shared slug-generation used by both self-service registration (AuthService) and
 * platform-admin onboarding (AdminCompanyService) — extracted so the two creation
 * paths cannot drift into subtly different slug rules.
 */
public final class CompanySlugGenerator {

    private CompanySlugGenerator() {
    }

    public static String generateUnique(String name, CompanyRepository companyRepository) {
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
