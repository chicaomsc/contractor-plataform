package io.chicaodw.platform.gallery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.AbstractControllerTest;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.gallery.api.dto.FeatureRequest;
import io.chicaodw.platform.gallery.api.dto.GalleryResponse;
import io.chicaodw.platform.gallery.api.dto.ReorderRequest;
import io.chicaodw.platform.gallery.api.dto.UpdateGalleryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GalleryControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /gallery ──────────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/gallery"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_nonOwner_returns403() throws Exception {
        mockMvc.perform(get("/gallery").with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_authenticated_returns200WithItems() throws Exception {
        when(galleryService.listItems(COMPANY_ID)).thenReturn(List.of(galleryResponse("Antes e Depois")));

        mockMvc.perform(get("/gallery").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Antes e Depois"));
    }

    // ── POST /gallery ─────────────────────────────────────────────────────────

    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/gallery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGalleryRequest("Obra", null, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_nonOwner_returns403() throws Exception {
        mockMvc.perform(post("/gallery")
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGalleryRequest("Obra", null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/gallery")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGalleryRequest("", null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        var request  = new CreateGalleryRequest("Renovação Completa", "Antes e depois da obra", 0, false, null);
        var response = galleryResponse("Renovação Completa");

        when(galleryService.createItem(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(post("/gallery")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Renovação Completa"));
    }

    // ── PUT /gallery/{id} ─────────────────────────────────────────────────────

    @Test
    void update_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/gallery/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_unknownItem_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(galleryService.updateItem(eq(COMPANY_ID), eq(id), any()))
                .thenThrow(new ResourceNotFoundException("GalleryItem", id));

        mockMvc.perform(put("/gallery/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var request  = new UpdateGalleryRequest("Título Actualizado", null, null, null, null);
        var response = galleryResponse("Título Actualizado");

        when(galleryService.updateItem(eq(COMPANY_ID), eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/gallery/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título Actualizado"));
    }

    // ── DELETE /gallery/{id} ──────────────────────────────────────────────────

    @Test
    void delete_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/gallery/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_unknownItem_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("GalleryItem", id))
                .when(galleryService).deleteItem(COMPANY_ID, id);

        mockMvc.perform(delete("/gallery/{id}", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingItem_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/gallery/{id}", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNoContent());

        verify(galleryService).deleteItem(COMPANY_ID, id);
    }

    // ── PATCH /gallery/{id}/feature ───────────────────────────────────────────

    @Test
    void feature_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/gallery/{id}/feature", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FeatureRequest(true))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void feature_nullFeatured_returns400() throws Exception {
        mockMvc.perform(patch("/gallery/{id}/feature", UUID.randomUUID())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feature_validRequest_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var response = galleryResponse("Obra Destaque");
        when(galleryService.feature(COMPANY_ID, id, true)).thenReturn(response);

        mockMvc.perform(patch("/gallery/{id}/feature", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FeatureRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Obra Destaque"));
    }

    // ── PATCH /gallery/{id}/reorder ───────────────────────────────────────────

    @Test
    void reorder_validOrder_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var response = galleryResponse("Obra");
        when(galleryService.reorder(COMPANY_ID, id, 2)).thenReturn(response);

        mockMvc.perform(patch("/gallery/{id}/reorder", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderRequest(2))))
                .andExpect(status().isOk());
    }

    // ── POST /gallery/{id}/before-image ───────────────────────────────────────

    @Test
    void uploadBeforeImage_noAuth_returns401() throws Exception {
        var file = new MockMultipartFile("file", "before.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/gallery/{id}/before-image", UUID.randomUUID()).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadBeforeImage_nonOwner_returns403() throws Exception {
        var file = new MockMultipartFile("file", "before.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/gallery/{id}/before-image", UUID.randomUUID()).file(file)
                        .with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadBeforeImage_validImage_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var file     = new MockMultipartFile("file", "before.jpg", "image/jpeg", new byte[]{1, 2, 3});
        var response = galleryResponseWithImages("Obra", "/uploads/before.jpg", null);

        when(galleryService.uploadBeforeImage(eq(COMPANY_ID), eq(id), any())).thenReturn(response);

        mockMvc.perform(multipart("/gallery/{id}/before-image", id).file(file)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beforeImageUrl").isNotEmpty());
    }

    // ── POST /gallery/{id}/after-image ────────────────────────────────────────

    @Test
    void uploadAfterImage_validImage_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var file     = new MockMultipartFile("file", "after.jpg", "image/jpeg", new byte[]{1, 2, 3});
        var response = galleryResponseWithImages("Obra", null, "/uploads/after.jpg");

        when(galleryService.uploadAfterImage(eq(COMPANY_ID), eq(id), any())).thenReturn(response);

        mockMvc.perform(multipart("/gallery/{id}/after-image", id).file(file)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afterImageUrl").isNotEmpty());
    }

    // ── DELETE /gallery/{id}/before-image ─────────────────────────────────────

    @Test
    void deleteBeforeImage_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/gallery/{id}/before-image", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteBeforeImage_authenticated_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var response = galleryResponse("Obra");
        when(galleryService.deleteBeforeImage(COMPANY_ID, id)).thenReturn(response);

        mockMvc.perform(delete("/gallery/{id}/before-image", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beforeImageUrl").doesNotExist());
    }

    // ── DELETE /gallery/{id}/after-image ──────────────────────────────────────

    @Test
    void deleteAfterImage_authenticated_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var response = galleryResponse("Obra");
        when(galleryService.deleteAfterImage(COMPANY_ID, id)).thenReturn(response);

        mockMvc.perform(delete("/gallery/{id}/after-image", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static GalleryResponse galleryResponse(String title) {
        return galleryResponseWithImages(title, null, null);
    }

    private static GalleryResponse galleryResponseWithImages(String title, String beforeUrl, String afterUrl) {
        return new GalleryResponse(
                UUID.randomUUID(), COMPANY_ID,
                title, null,
                beforeUrl, afterUrl,
                0, false, true,
                Instant.now(), Instant.now()
        );
    }
}
