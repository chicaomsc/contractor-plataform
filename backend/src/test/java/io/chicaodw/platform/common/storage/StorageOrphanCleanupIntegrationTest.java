package io.chicaodw.platform.common.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.RegisterRequest;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for Sprint 11B.6C item 3 (orphan-file cleanup on replace/delete)
 * using the real {@link LocalStorageService} — files are genuinely written/removed on
 * disk under {@code ./storage}, so these assertions prove the actual ordering, not just
 * mocked call order.
 */
@AutoConfigureMockMvc
class StorageOrphanCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StorageProperties storageProperties;

    @Test
    void replacingLogo_removesOldFile_keepsNewFile() throws Exception {
        String accessToken = registerAndGetToken();

        String firstUrl = uploadLogo(accessToken).get("logoUrl").asText();
        assertThat(existsOnDisk(firstUrl)).isTrue();

        String secondUrl = uploadLogo(accessToken).get("logoUrl").asText();
        assertThat(secondUrl).isNotEqualTo(firstUrl);
        assertThat(existsOnDisk(secondUrl)).isTrue();
        assertThat(existsOnDisk(firstUrl)).isFalse();
    }

    @Test
    void deletingLogo_removesFileFromDisk() throws Exception {
        String accessToken = registerAndGetToken();
        String url = uploadLogo(accessToken).get("logoUrl").asText();
        assertThat(existsOnDisk(url)).isTrue();

        mockMvc.perform(delete("/company/logo").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(existsOnDisk(url)).isFalse();
    }

    @Test
    void replacingGalleryBeforeImage_removesOldFile_keepsNewFile() throws Exception {
        String accessToken = registerAndGetToken();
        String itemId = createGalleryItem(accessToken);

        String firstUrl = uploadBeforeImage(accessToken, itemId).get("beforeImageUrl").asText();
        assertThat(existsOnDisk(firstUrl)).isTrue();

        String secondUrl = uploadBeforeImage(accessToken, itemId).get("beforeImageUrl").asText();
        assertThat(secondUrl).isNotEqualTo(firstUrl);
        assertThat(existsOnDisk(secondUrl)).isTrue();
        assertThat(existsOnDisk(firstUrl)).isFalse();
    }

    @Test
    void deletingGalleryItem_removesBothAssociatedImages() throws Exception {
        String accessToken = registerAndGetToken();
        String itemId = createGalleryItem(accessToken);
        String beforeUrl = uploadBeforeImage(accessToken, itemId).get("beforeImageUrl").asText();
        String afterUrl = uploadAfterImage(accessToken, itemId).get("afterImageUrl").asText();
        assertThat(existsOnDisk(beforeUrl)).isTrue();
        assertThat(existsOnDisk(afterUrl)).isTrue();

        mockMvc.perform(delete("/gallery/{id}", itemId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(existsOnDisk(beforeUrl)).isFalse();
        assertThat(existsOnDisk(afterUrl)).isFalse();
    }

    @Test
    void replacingLogo_survivesOldFileAlreadyMissingFromDisk() throws Exception {
        // Simulates a failed physical delete on a previous attempt (or manual ops
        // cleanup) — deleteQuietly must be a true no-op for an already-missing file,
        // never blocking the new reference that's already been persisted.
        String accessToken = registerAndGetToken();
        String firstUrl = uploadLogo(accessToken).get("logoUrl").asText();
        Files.deleteIfExists(diskPath(firstUrl));
        assertThat(existsOnDisk(firstUrl)).isFalse();

        var second = uploadLogo(accessToken);

        assertThat(second.get("logoUrl").asText()).isNotEqualTo(firstUrl);
        assertThat(existsOnDisk(second.get("logoUrl").asText())).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetToken() throws Exception {
        var req = new RegisterRequest("Owner", "orphan-" + System.nanoTime() + "@example.com",
                "securePass1", "Orphan Cleanup Co " + System.nanoTime(), "PT");
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class).accessToken();
    }

    private String createGalleryItem(String accessToken) throws Exception {
        var req = new CreateGalleryRequest("Obra Teste", null, 0, false, true);
        String body = mockMvc.perform(post("/gallery")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private com.fasterxml.jackson.databind.JsonNode uploadLogo(String accessToken) throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes());
        String body = mockMvc.perform(multipart("/company/logo").file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private com.fasterxml.jackson.databind.JsonNode uploadBeforeImage(String accessToken, String itemId) throws Exception {
        var file = new MockMultipartFile("file", "before.png", "image/png", pngBytes());
        String body = mockMvc.perform(multipart("/gallery/{id}/before-image", itemId).file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private com.fasterxml.jackson.databind.JsonNode uploadAfterImage(String accessToken, String itemId) throws Exception {
        var file = new MockMultipartFile("file", "after.png", "image/png", pngBytes());
        String body = mockMvc.perform(multipart("/gallery/{id}/after-image", itemId).file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private boolean existsOnDisk(String storedPath) {
        return Files.exists(diskPath(storedPath));
    }

    private Path diskPath(String storedPath) {
        Path baseDir = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();
        return baseDir.resolve(storedPath.replaceFirst("^/uploads/", ""));
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(6, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 6, 4);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
