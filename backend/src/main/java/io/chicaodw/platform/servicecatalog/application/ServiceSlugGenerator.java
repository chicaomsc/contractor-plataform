package io.chicaodw.platform.servicecatalog.application;

import io.chicaodw.platform.servicecatalog.infrastructure.persistence.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.UUID;

/**
 * Domain service responsible for generating unique, URL-safe slugs for catalogue services.
 * Uniqueness is scoped per company: the same slug may exist across different companies.
 */
@Component
@RequiredArgsConstructor
public class ServiceSlugGenerator {

    private final ServiceRepository serviceRepository;

    public String generate(String name, UUID companyId) {
        String base = toSlug(name);
        if (!serviceRepository.existsByCompanyIdAndSlug(companyId, base)) {
            return base;
        }
        int counter = 2;
        String candidate;
        do {
            candidate = base + "-" + counter++;
        } while (serviceRepository.existsByCompanyIdAndSlug(companyId, candidate));
        return candidate;
    }

    private static String toSlug(String name) {
        // Normalise accented characters, then strip non-ASCII
        String normalised = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                                      .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalised.toLowerCase()
                         .replaceAll("[^a-z0-9\\s-]", "")
                         .replaceAll("\\s+", "-")
                         .replaceAll("-{2,}", "-")
                         .replaceAll("^-|-$", "");
    }
}
