package io.chicaodw.platform.common.storage;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

/**
 * Centralized image pipeline shared by every upload flow (company logo, gallery
 * before/after photos) — the single place that decides how an accepted image (PNG,
 * JPEG, or WebP; already passed through {@link ImageUploadPolicy}) is turned into the
 * bytes that actually get persisted (Sprint 11B.6C, SEC-STORAGE-02).
 *
 * Every image is fully decoded and re-encoded from a fresh {@link BufferedImage} — this
 * is what removes EXIF/XMP/ICC metadata, since a re-encoded image only ever carries the
 * pixel data we explicitly wrote, never a copy of the original file's metadata segments.
 * Before discarding it, the EXIF orientation tag (if any) is read and "baked into" the
 * pixel data by rotating/flipping, so the stored image displays correctly in viewers
 * that (unlike most browsers) don't apply EXIF orientation themselves.
 *
 * WebP input is decoded (via the TwelveMonkeys WebP {@code ImageReader}, registered on
 * the classpath) but always re-encoded as PNG: the JDK ships no WebP {@code ImageWriter}
 * and every actively maintained pure-Java option we could add is read-only, so writing
 * WebP back out would require a native-code dependency. PNG is lossless and (unlike
 * JPEG) preserves the alpha channel WebP can carry, so no visual quality is lost —
 * only the on-disk format changes. PNG and JPEG uploads keep their original format.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageNormalizationService {

    private final ImageNormalizationProperties properties;

    public NormalizedImage normalize(MultipartFile file) {
        byte[] rawBytes = readBytes(file);

        try (InputStream in = new ByteArrayInputStream(rawBytes);
             ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) {
                throw decodeError();
            }

            ImageReader reader = firstReaderFor(iis);
            try {
                reader.setInput(iis, true, true);
                validateDimensions(reader);

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw decodeError();
                }

                BufferedImage oriented = applyOrientation(decoded, readExifOrientation(rawBytes));
                return encode(oriented, reader.getFormatName());
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw decodeError();
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Image file could not be read");
        }
    }

    private ImageReader firstReaderFor(ImageInputStream iis) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            throw decodeError();
        }
        return readers.next();
    }

    private void validateDimensions(ImageReader reader) throws IOException {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        long pixels = (long) width * (long) height;

        if (width > properties.getMaxWidthPx() || height > properties.getMaxHeightPx()
                || pixels > properties.getMaxPixels()) {
            throw new BusinessRuleException("Image dimensions exceed the allowed maximum");
        }
    }

    private int readExifOrientation(byte[] rawBytes) {
        try {
            var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(rawBytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            log.debug("No usable EXIF orientation metadata found — treating image as already upright", e);
        }
        return 1;
    }

    private static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        return switch (orientation) {
            case 2 -> flipHorizontal(src);
            case 3 -> rotate180(src);
            case 4 -> flipVertical(src);
            case 5 -> flipHorizontal(rotate90Cw(src));
            case 6 -> rotate90Cw(src);
            case 7 -> flipHorizontal(rotate270Cw(src));
            case 8 -> rotate270Cw(src);
            default -> src;
        };
    }

    private static BufferedImage rotate90Cw(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, resolveType(src));
        Graphics2D g = dst.createGraphics();
        g.translate(h, 0);
        g.rotate(Math.PI / 2);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage rotate270Cw(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, resolveType(src));
        Graphics2D g = dst.createGraphics();
        g.translate(0, w);
        g.rotate(-Math.PI / 2);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage rotate180(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, resolveType(src));
        Graphics2D g = dst.createGraphics();
        g.translate(w, h);
        g.rotate(Math.PI);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage flipHorizontal(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, resolveType(src));
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, w, 0, -w, h, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage flipVertical(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, resolveType(src));
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, h, w, -h, null);
        g.dispose();
        return dst;
    }

    private static int resolveType(BufferedImage src) {
        return src.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : src.getType();
    }

    private NormalizedImage encode(BufferedImage image, String sourceFormatName) {
        String lower = sourceFormatName == null ? "" : sourceFormatName.toLowerCase(Locale.ROOT);
        if (lower.contains("jpeg") || lower.contains("jpg")) {
            return encodeJpeg(image);
        }
        return encodePng(image);
    }

    private NormalizedImage encodeJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw encodeError();
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(properties.getJpegQuality());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return new NormalizedImage(out.toByteArray(), "jpg");
        } catch (IOException e) {
            throw encodeError();
        } finally {
            writer.dispose();
        }
    }

    private NormalizedImage encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw encodeError();
            }
            return new NormalizedImage(out.toByteArray(), "png");
        } catch (IOException e) {
            throw encodeError();
        }
    }

    private static BusinessRuleException decodeError() {
        return new BusinessRuleException("Image file could not be decoded");
    }

    private static BusinessRuleException encodeError() {
        return new BusinessRuleException("Failed to process the uploaded image");
    }
}
