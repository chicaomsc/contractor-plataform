package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ImageUploadPolicy {

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final Map<String, Set<String>> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/png", Set.of("png"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/webp", Set.of("webp")
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file cannot be empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessRuleException("Image file exceeds the maximum size of 5 MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Only PNG, JPEG and WebP images are accepted");
        }

        String extension = extension(file.getOriginalFilename());
        if (!EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, Set.of()).contains(extension)) {
            throw new BusinessRuleException("Image file extension does not match the content type");
        }

        if (!hasExpectedSignature(file, contentType)) {
            throw new BusinessRuleException("Image file content does not match the declared type");
        }
    }

    private static String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean hasExpectedSignature(MultipartFile file, String contentType) {
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (contentType) {
                case "image/png" -> startsWith(header, new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                });
                case "image/jpeg" -> header.length >= 3
                        && header[0] == (byte) 0xFF
                        && header[1] == (byte) 0xD8
                        && header[2] == (byte) 0xFF;
                case "image/webp" -> header.length >= 12
                        && header[0] == 0x52
                        && header[1] == 0x49
                        && header[2] == 0x46
                        && header[3] == 0x46
                        && header[8] == 0x57
                        && header[9] == 0x45
                        && header[10] == 0x42
                        && header[11] == 0x50;
                default -> false;
            };
        } catch (IOException e) {
            throw new BusinessRuleException("Image file could not be read");
        }
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }

        for (int index = 0; index < prefix.length; index += 1) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }

        return true;
    }
}
