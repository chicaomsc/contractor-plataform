package io.chicaodw.platform.gallery;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.storage.ImageUploadPolicy;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.gallery.application.GalleryImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryImageServiceTest {

    @Mock StorageService storageService;
    @Spy ImageUploadPolicy imageUploadPolicy = new ImageUploadPolicy();
    @InjectMocks GalleryImageService galleryImageService;

    private final UUID companyId = UUID.randomUUID();

    // ── storeImage — validation ───────────────────────────────────────────────

    @Test
    void storeImage_emptyFile_throwsBusinessRule() {
        var file = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void storeImage_nullFile_throwsBusinessRule() {
        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void storeImage_nonImageContentType_throwsBusinessRule() {
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("image");
    }

    @Test
    void storeImage_noContentType_throwsBusinessRule() {
        var file = new MockMultipartFile("file", "image.png", null, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("image");
    }

    // ── storeImage — happy path ───────────────────────────────────────────────

    @Test
    void storeImage_validJpeg_delegatesToStorageWithCorrectFolder() {
        var file = new MockMultipartFile("file", "before.jpg", "image/jpeg", jpegBytes());
        String expectedPath = "/uploads/company/" + companyId + "/gallery/uuid.jpg";
        when(storageService.store(any(), any())).thenReturn(expectedPath);

        String result = galleryImageService.storeImage(companyId, file);

        assertThat(result).isEqualTo(expectedPath);
        verify(storageService).store(eq("company/" + companyId + "/gallery"), eq(file));
    }

    @Test
    void storeImage_validPng_delegatesToStorage() {
        var file = new MockMultipartFile("file", "after.png", "image/png", pngBytes());
        when(storageService.store(any(), any())).thenReturn("/uploads/company/" + companyId + "/gallery/uuid.png");

        galleryImageService.storeImage(companyId, file);

        verify(storageService).store("company/" + companyId + "/gallery", file);
    }

    // ── deleteImage ───────────────────────────────────────────────────────────

    @Test
    void deleteImage_delegatesToStorage() {
        String path = "/uploads/company/" + companyId + "/gallery/image.jpg";

        galleryImageService.deleteImage(path);

        verify(storageService).delete(path);
    }

    @Test
    void storeImage_svgIsRejected() {
        var file = new MockMultipartFile("file", "icon.svg", "image/svg+xml", "<svg />".getBytes());

        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PNG, JPEG and WebP");
    }

    @Test
    void storeImage_spoofedPngIsRejected() {
        var file = new MockMultipartFile("file", "image.png", "image/png", "not really a png".getBytes());

        assertThatThrownBy(() -> galleryImageService.storeImage(companyId, file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("content");
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };
    }

    private static byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    }
}
