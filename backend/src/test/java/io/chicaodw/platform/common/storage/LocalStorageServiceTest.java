package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.common.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void store_rejectsFolderTraversal() {
        var service = service();
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> service.store("../outside", file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Invalid storage folder");
    }

    @Test
    void delete_rejectsPathTraversal() {
        var service = service();

        assertThatThrownBy(() -> service.delete("/uploads/../../outside.png"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Invalid stored path");
    }

    @Test
    void store_writesInsideBaseDirectoryWithGeneratedName() throws Exception {
        var service = service();
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2, 3});

        String storedPath = service.store("company/test/gallery", file);

        assertThat(storedPath).startsWith("/uploads/company/test/gallery/");
        assertThat(Files.exists(tempDir.resolve(storedPath.replaceFirst("^/uploads/", "")))).isTrue();
    }

    private LocalStorageService service() {
        var properties = new StorageProperties();
        properties.setBasePath(tempDir.toString());
        return new LocalStorageService(properties);
    }
}
