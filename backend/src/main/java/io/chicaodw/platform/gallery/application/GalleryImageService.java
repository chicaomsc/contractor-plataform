package io.chicaodw.platform.gallery.application;

import io.chicaodw.platform.common.storage.ImageUploadPolicy;
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

    public String storeImage(UUID companyId, MultipartFile file) {
        imageUploadPolicy.validate(file);
        return storageService.store("company/" + companyId + "/gallery", file);
    }

    public void deleteImage(String storedPath) {
        storageService.delete(storedPath);
    }

}
