package io.chicaodw.platform.common.storage;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageNormalizationServiceTest {

    private final ImageNormalizationProperties properties = new ImageNormalizationProperties();
    private final ImageNormalizationService service = new ImageNormalizationService(properties);

    @Test
    void normalize_validPng_isDecodableAndKeepsPngExtension() throws IOException {
        var file = pngFile(solidColorImage(10, 8, Color.BLUE));

        NormalizedImage result = service.normalize(file);

        assertThat(result.extension()).isEqualTo("png");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(10);
        assertThat(decoded.getHeight()).isEqualTo(8);
    }

    @Test
    void normalize_validJpeg_isDecodableAndKeepsJpegExtension() throws IOException {
        var file = jpegFile(solidColorImage(10, 8, Color.GREEN));

        NormalizedImage result = service.normalize(file);

        assertThat(result.extension()).isEqualTo("jpg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(10);
        assertThat(decoded.getHeight()).isEqualTo(8);
    }

    @Test
    void normalize_webp_decodesAndConvertsToPng() throws IOException {
        // Real 4x2 lossless WebP fixture (generated with Pillow — hand-writing a valid
        // WebP bitstream isn't practical, unlike PNG/JPEG which ImageIO can write itself).
        byte[] webpBytes;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/tiny.webp")) {
            assertThat(in).as("test fixture fixtures/tiny.webp must exist").isNotNull();
            webpBytes = in.readAllBytes();
        }
        var file = new MockMultipartFile("file", "photo.webp", "image/webp", webpBytes);

        NormalizedImage result = service.normalize(file);

        assertThat(result.extension()).isEqualTo("png");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(4);
        assertThat(decoded.getHeight()).isEqualTo(2);
    }

    @Test
    void normalize_jpegWithExifOrientation_bakesRotationIntoPixels() throws IOException {
        // 6x4 landscape source, orientation=6 ("rotate 90° CW to display upright") —
        // a correctly oriented output must therefore be 4x6 (portrait): the reader
        // itself never rotates pixels, only ImageNormalizationService does.
        byte[] jpegWithExif = jpegWithExifOrientation(solidColorImage(6, 4, Color.GREEN), 6);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegWithExif);

        NormalizedImage result = service.normalize(file);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(4);
        assertThat(decoded.getHeight()).isEqualTo(6);
    }

    @Test
    void normalize_jpegWithoutExif_isNotRotated() throws IOException {
        var file = jpegFile(solidColorImage(6, 4, Color.GREEN));

        NormalizedImage result = service.normalize(file);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(6);
        assertThat(decoded.getHeight()).isEqualTo(4);
    }

    @Test
    void normalize_malformedImage_throwsBusinessRule() {
        // Passes ImageUploadPolicy-style magic bytes but isn't a real decodable PNG.
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        var file = new MockMultipartFile("file", "image.png", "image/png", fakePng);

        assertThatThrownBy(() -> service.normalize(file))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void normalize_dimensionsExceedConfiguredMaximum_throwsBusinessRule() throws IOException {
        var tightProperties = new ImageNormalizationProperties();
        tightProperties.setMaxWidthPx(10);
        tightProperties.setMaxHeightPx(10);
        tightProperties.setMaxPixels(100);
        var tightService = new ImageNormalizationService(tightProperties);

        var file = pngFile(solidColorImage(20, 20, Color.BLUE));

        assertThatThrownBy(() -> tightService.normalize(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void normalize_dimensionsWithinLimit_isAccepted() throws IOException {
        var tightProperties = new ImageNormalizationProperties();
        tightProperties.setMaxWidthPx(10);
        tightProperties.setMaxHeightPx(10);
        tightProperties.setMaxPixels(100);
        var tightService = new ImageNormalizationService(tightProperties);

        var file = pngFile(solidColorImage(5, 5, Color.BLUE));

        NormalizedImage result = tightService.normalize(file);

        assertThat(result.bytes()).isNotEmpty();
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static BufferedImage solidColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static MockMultipartFile pngFile(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "image.png", "image/png", out.toByteArray());
    }

    private static MockMultipartFile jpegFile(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("file", "image.jpg", "image/jpeg", out.toByteArray());
    }

    /**
     * Splices a minimal hand-built EXIF APP1 segment (TIFF/IFD0, a single Orientation
     * tag) right after the JPEG SOI marker of an ImageIO-encoded JPEG. There is no
     * simpler way to get a JPEG with a specific EXIF orientation tag for a test: the
     * JDK's JPEG writer never writes EXIF, and metadata-extractor (used by
     * ImageNormalizationService) only reads it.
     */
    private static byte[] jpegWithExifOrientation(BufferedImage image, int orientation) throws IOException {
        byte[] plain = jpegBytes(image);

        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write('M'); tiff.write('M');                                   // big-endian
        tiff.write(0x00); tiff.write(0x2A);                                 // TIFF magic
        tiff.write(0x00); tiff.write(0x00); tiff.write(0x00); tiff.write(0x08); // offset to IFD0
        tiff.write(0x00); tiff.write(0x01);                                 // 1 entry
        tiff.write(0x01); tiff.write(0x12);                                 // tag 0x0112 Orientation
        tiff.write(0x00); tiff.write(0x03);                                 // type SHORT
        tiff.write(0x00); tiff.write(0x00); tiff.write(0x00); tiff.write(0x01); // count 1
        tiff.write(0x00); tiff.write(orientation & 0xFF);                  // value (big-endian SHORT)
        tiff.write(0x00); tiff.write(0x00);                                 // padding
        tiff.write(0x00); tiff.write(0x00); tiff.write(0x00); tiff.write(0x00); // next IFD = none

        ByteArrayOutputStream app1Payload = new ByteArrayOutputStream();
        app1Payload.writeBytes("Exif\0\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        app1Payload.writeBytes(tiff.toByteArray());
        byte[] payload = app1Payload.toByteArray();
        int length = payload.length + 2;

        ByteArrayOutputStream segment = new ByteArrayOutputStream();
        segment.write(0xFF); segment.write(0xE1);
        segment.write((length >> 8) & 0xFF); segment.write(length & 0xFF);
        segment.writeBytes(payload);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(plain, 0, 2); // SOI
        result.writeBytes(segment.toByteArray());
        result.write(plain, 2, plain.length - 2);
        return result.toByteArray();
    }

    private static byte[] jpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
