package io.chicaodw.platform.common.storage;

import java.util.List;

/**
 * Result of {@link StorageReconciliationService#findOrphans} — report-only, never
 * deletes anything (Sprint 11B.6C item 4: "não apagar automaticamente em produção
 * nesta sprint").
 *
 * @param totalFilesOnDisk total regular files found under the storage root
 * @param totalReferenced  total distinct storage keys referenced by branding/gallery rows
 * @param orphanPaths      storage keys present on disk but referenced by no row
 */
public record StorageOrphanReport(int totalFilesOnDisk, int totalReferenced, List<String> orphanPaths) {
}
