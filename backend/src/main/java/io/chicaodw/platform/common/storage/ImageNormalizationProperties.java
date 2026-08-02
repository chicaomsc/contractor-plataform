package io.chicaodw.platform.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits enforced by {@link ImageNormalizationService} before an uploaded image is
 * decoded/re-encoded (Sprint 11B.6C, SEC-STORAGE-02). Dimension/pixel caps exist to
 * reject decompression-bomb-style images (small file, enormous decoded dimensions)
 * before {@code ImageIO} allocates the full pixel buffer.
 */
@ConfigurationProperties(prefix = "app.image-normalization")
@Getter
@Setter
public class ImageNormalizationProperties {

    private int maxWidthPx = 6000;
    private int maxHeightPx = 6000;
    private long maxPixels = 30_000_000L;
    private float jpegQuality = 0.85f;
}
