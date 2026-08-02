package io.chicaodw.platform.common.storage;

/**
 * Result of {@link ImageNormalizationService#normalize} — bytes freshly re-encoded from
 * a decoded {@code BufferedImage}, carrying no metadata beyond pixel data (no EXIF, no
 * XMP, no ICC profile) and already oriented correctly for display.
 *
 * @param bytes     the re-encoded image content, ready to hand to {@link StorageService#store}
 * @param extension file extension without a leading dot, e.g. "png" or "jpg"
 */
public record NormalizedImage(byte[] bytes, String extension) {
}
