package io.chicaodw.platform.common.storage;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for file storage.
 * Implementations may target local disk, Supabase, S3, etc.
 *
 * Deliberately framework/filesystem-agnostic: content moves as {@code byte[]}, and every
 * stored file is addressed by a storage key ({@code String}, e.g.
 * {@code "/uploads/company/{id}/logo/{uuid}.png"}) — never a {@code java.nio.file.Path}
 * or a Spring-web {@code MultipartFile}. This is what lets a future S3-backed
 * implementation exist without touching any caller (Sprint 11B.6C item 6).
 */
public interface StorageService {

    /**
     * Stores already-processed bytes under the given folder path and returns the
     * publicly addressable URL/path to be persisted in the database. The caller is
     * responsible for any validation/normalization — this method just writes bytes.
     *
     * @param folder    relative folder, e.g. "company/{id}/logo"
     * @param content   the file's bytes, exactly as they will be served
     * @param extension file extension without a leading dot, e.g. "png"
     * @return absolute or relative URL
     */
    String store(String folder, byte[] content, String extension);

    /**
     * Deletes the file identified by the URL previously returned by {@link #store}.
     * No-op if the path is null or the file no longer exists.
     *
     * @param storedPath value previously returned by {@code store}
     */
    void delete(String storedPath);

    /**
     * Best-effort deletion — used when a file is being superseded/orphaned as a
     * consequence of a database write that has already succeeded (replace/delete
     * flows). Never throws: a failed physical delete must not roll back or block the
     * transaction whose new state has already been persisted (DT-011B.5, Sprint
     * 11B.6C item 3). Implementations must catch and log rather than propagate.
     *
     * @param storedPath value previously returned by {@code store}
     */
    void deleteQuietly(String storedPath);

    /**
     * Reads the bytes of a file previously stored by {@link #store}. Never throws for a
     * missing or unreadable file — returns empty so callers (e.g. PDF generation) can
     * degrade gracefully instead of failing the whole operation over a missing logo.
     *
     * @param storedPath value previously returned by {@code store}
     * @return the file's bytes, or empty if {@code storedPath} is null/blank/unresolvable
     */
    Optional<byte[]> load(String storedPath);

    /**
     * Lists the storage keys (in the same shape returned by {@link #store}) of every
     * file currently stored under a folder prefix. Used only by
     * {@code StorageReconciliationService} for orphan detection — not part of any
     * request-serving hot path. Returns an empty list for a missing/invalid prefix,
     * never throws.
     *
     * @param folderPrefix relative folder prefix, e.g. "company/{id}"; blank/null lists everything
     * @return storage keys of every regular file found, in no particular order
     */
    List<String> list(String folderPrefix);
}
