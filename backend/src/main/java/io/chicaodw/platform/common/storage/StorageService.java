package io.chicaodw.platform.common.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Strategy interface for file storage.
 * Implementations may target local disk, Supabase, S3, etc.
 */
public interface StorageService {

    /**
     * Stores a file under the given folder path and returns the publicly
     * addressable URL/path to be persisted in the database.
     *
     * @param folder relative folder, e.g. "company/{id}/logo"
     * @param file   the file to store
     * @return absolute or relative URL
     */
    String store(String folder, MultipartFile file);

    /**
     * Deletes the file identified by the URL previously returned by {@link #store}.
     * No-op if the path is null or the file no longer exists.
     *
     * @param storedPath value previously returned by {@code store}
     */
    void delete(String storedPath);

    /**
     * Reads the bytes of a file previously stored by {@link #store}. Never throws for a
     * missing or unreadable file — returns empty so callers (e.g. PDF generation) can
     * degrade gracefully instead of failing the whole operation over a missing logo.
     *
     * @param storedPath value previously returned by {@code store}
     * @return the file's bytes, or empty if {@code storedPath} is null/blank/unresolvable
     */
    Optional<byte[]> load(String storedPath);
}
