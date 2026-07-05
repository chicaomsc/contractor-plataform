package io.chicaodw.platform.gallery.application;

import io.chicaodw.platform.common.exception.BusinessRuleException;
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

    public String storeImage(UUID companyId, MultipartFile file) {
        validate(file);
        return storageService.store("company/" + companyId + "/gallery", file);
    }

    public void deleteImage(String storedPath) {
        storageService.delete(storedPath);
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file cannot be empty");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new BusinessRuleException("Only image files are accepted");
        }
    }
}
