package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.gallery.domain.GalleryItem;
import io.chicaodw.platform.gallery.infrastructure.persistence.GalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manual, read-only reconciliation between what's actually on disk under
 * {@code StorageProperties.basePath} and what the database still references
 * ({@code Branding.logoUrl}, {@code GalleryItem.beforeImageUrl}/{@code afterImageUrl}) —
 * Sprint 11B.6C item 4 (SEC-STORAGE-05's compensation gap: storage writes and DB writes
 * aren't transactional with each other, so orphans can still occur even with the
 * best-effort cleanup added in item 3 — e.g. process crash between the two writes).
 *
 * Deliberately report-only: this sprint does not wire up any automatic or scheduled
 * deletion. Orphans found here are candidates for a human to review and delete via the
 * ops runbook, not for this service to remove itself — see
 * {@code docs/operations/runbook.md} for the manual cleanup procedure.
 */
@Service
@RequiredArgsConstructor
public class StorageReconciliationService {

    private final StorageService storageService;
    private final BrandingRepository brandingRepository;
    private final GalleryRepository galleryRepository;

    @Transactional(readOnly = true)
    public StorageOrphanReport findOrphans() {
        List<String> filesOnDisk = storageService.list(null);

        Set<String> referenced = new HashSet<>();
        for (Branding branding : brandingRepository.findAll()) {
            addIfPresent(referenced, branding.getLogoUrl());
        }
        for (GalleryItem item : galleryRepository.findAll()) {
            addIfPresent(referenced, item.getBeforeImageUrl());
            addIfPresent(referenced, item.getAfterImageUrl());
        }

        List<String> orphans = filesOnDisk.stream()
                .filter(path -> !referenced.contains(path))
                .toList();

        return new StorageOrphanReport(filesOnDisk.size(), referenced.size(), orphans);
    }

    private static void addIfPresent(Set<String> target, String url) {
        if (url != null) {
            target.add(url);
        }
    }
}
