package io.chicaodw.platform.admin;

import io.chicaodw.platform.common.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Sprint 11B.6C item 4 — the SUPER_ADMIN-only, report-only orphan endpoint.
 */
class StorageAdminControllerTest extends AbstractAdminIntegrationTest {

    @Autowired StorageProperties storageProperties;

    @Test
    void listOrphans_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/admin/storage/orphans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrphans_owner_returns403() throws Exception {
        String ownerToken = registerOwnerAndLogin();

        mockMvc.perform(get("/admin/storage/orphans").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrphans_superAdmin_reportsUnreferencedFileButNeverDeletesIt() throws Exception {
        String adminToken = createSuperAdminAndLogin();
        var owner = registerOwner();

        var file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes());
        String logoBody = mockMvc.perform(multipart("/company/logo").file(file)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String referencedUrl = objectMapper.readTree(logoBody).get("logoUrl").asText();

        Path orphanFile = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize()
                .resolve("company/unreferenced-orphan-test/logo/orphan.png");
        Files.createDirectories(orphanFile.getParent());
        Files.write(orphanFile, pngBytes());
        String orphanUrl = "/uploads/company/unreferenced-orphan-test/logo/orphan.png";

        try {
            mockMvc.perform(get("/admin/storage/orphans").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orphanPaths").value(org.hamcrest.Matchers.hasItem(orphanUrl)))
                    .andExpect(jsonPath("$.orphanPaths").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(referencedUrl))));

            // report-only: the orphan file must still be there after the call.
            org.assertj.core.api.Assertions.assertThat(Files.exists(orphanFile)).isTrue();
        } finally {
            Files.deleteIfExists(orphanFile);
        }
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
