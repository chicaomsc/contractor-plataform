package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.common.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j

@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;

    @Override
    public String store(String folder, MultipartFile file) {
        try {
            Path baseDir = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
            Path targetDir = baseDir.resolve(folder).normalize();
            if (!targetDir.startsWith(baseDir)) {
                throw new StorageException("Invalid storage folder", null);
            }
            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + extension(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file in folder: " + folder, e);
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;
        try {
            if (!storedPath.startsWith("/uploads/")) {
                throw new StorageException("Invalid stored path", null);
            }
            Path baseDir = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
            String relative = storedPath.replaceFirst("^/uploads/", "");
            Path target = baseDir.resolve(relative).normalize();
            if (!target.startsWith(baseDir)) {
                throw new StorageException("Invalid stored path", null);
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + storedPath, e);
        }
    }

    @Override
    public Optional<byte[]> load(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || !storedPath.startsWith("/uploads/")) {
            return Optional.empty();
        }
        try {
            Path baseDir = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
            String relative = storedPath.replaceFirst("^/uploads/", "");
            Path target = baseDir.resolve(relative).normalize();
            if (!target.startsWith(baseDir) || !Files.isRegularFile(target)) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException e) {
            log.warn("Failed to read stored file: {}", storedPath, e);
            return Optional.empty();
        }
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return "." + filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
