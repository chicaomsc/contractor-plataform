package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.common.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void store_rejectsFolderTraversal() {
        var service = service();

        assertThatThrownBy(() -> service.store("../outside", new byte[]{1}, "png"))
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
    void store_writesInsideBaseDirectoryWithGeneratedName() {
        var service = service();

        String storedPath = service.store("company/test/gallery", new byte[]{1, 2, 3}, "png");

        assertThat(storedPath).startsWith("/uploads/company/test/gallery/").endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(storedPath.replaceFirst("^/uploads/", "")))).isTrue();
    }

    @Test
    void deleteQuietly_neverThrows_evenForInvalidPath() {
        var service = service();

        assertThatNoException().isThrownBy(() -> service.deleteQuietly("/uploads/../../outside.png"));
        assertThatNoException().isThrownBy(() -> service.deleteQuietly(null));
    }

    @Test
    void deleteQuietly_removesFileJustLikeDelete() {
        var service = service();
        String storedPath = service.store("company/test/logo", new byte[]{1}, "png");

        service.deleteQuietly(storedPath);

        assertThat(Files.exists(tempDir.resolve(storedPath.replaceFirst("^/uploads/", "")))).isFalse();
    }

    @Test
    void list_returnsEveryStoredFileUnderPrefix() {
        var service = service();
        String a = service.store("company/test/logo", new byte[]{1}, "png");
        String b = service.store("company/test/gallery", new byte[]{2}, "jpg");
        service.store("company/other/logo", new byte[]{3}, "png");

        List<String> files = service.list("company/test");

        assertThat(files).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void list_rejectsPrefixTraversal() {
        var service = service();

        assertThat(service.list("../outside")).isEmpty();
    }

    @Test
    void list_missingFolder_returnsEmpty() {
        var service = service();

        assertThat(service.list("company/does-not-exist")).isEmpty();
    }

    private LocalStorageService service() {
        var properties = new StorageProperties();
        properties.setBasePath(tempDir.toString());
        return new LocalStorageService(properties);
    }
}
