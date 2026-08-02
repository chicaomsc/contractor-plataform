package io.chicaodw.platform.gallery.application;

import io.chicaodw.platform.common.storage.ImageNormalizationService;
import io.chicaodw.platform.common.storage.ImageUploadPolicy;
import io.chicaodw.platform.common.storage.NormalizedImage;
import io.chicaodw.platform.common.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Domain service responsible for gallery image lifecycle:
 * upload, validation, and deletion.
 * Never accesses the GalleryRepository — only the StorageService.
 */
@Service
@RequiredArgsConstructor
public class GalleryImageService {

    private final StorageService storageService;
    private final ImageUploadPolicy imageUploadPolicy;
    private final ImageNormalizationService imageNormalizationService;

    public String storeImage(UUID companyId, MultipartFile file) {
        imageUploadPolicy.validate(file);
        NormalizedImage normalized = imageNormalizationService.normalize(file);
        return storageService.store("company/" + companyId + "/gallery", normalized.bytes(), normalized.extension());
    }

    /**
     * Best-effort — a physical delete failure must never block/undo the caller's DB
     * write that already replaced/removed the reference to this file (DT-011B.5,
     * Sprint 11B.6C item 3).
     */
    public void deleteImage(String storedPath) {
        storageService.deleteQuietly(storedPath);
    }

}
