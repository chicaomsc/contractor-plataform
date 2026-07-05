package io.chicaodw.platform.servicecatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.AbstractControllerTest;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.dto.ReorderRequest;
import io.chicaodw.platform.servicecatalog.api.dto.ServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.UpdateServiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /services ─────────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_nonOwner_returns403() throws Exception {
        mockMvc.perform(get("/services").with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_authenticated_returns200WithItems() throws Exception {
        when(catalogService.listServices(COMPANY_ID)).thenReturn(List.of(serviceResponse("Pintura")));

        mockMvc.perform(get("/services").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pintura"));
    }

    // ── POST /services ────────────────────────────────────────────────────────

    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateServiceRequest("Pintura", null, null, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_nonOwner_returns403() throws Exception {
        mockMvc.perform(post("/services")
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateServiceRequest("Pintura", null, null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/services")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateServiceRequest("", null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void create_negativeDisplayOrder_returns400() throws Exception {
        mockMvc.perform(post("/services")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateServiceRequest("Pintura", null, null, null, -1, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        var request  = new CreateServiceRequest("Pintura Interior", "Serviço de pintura", null, null, 0, null);
        var response = serviceResponse("Pintura Interior");

        when(catalogService.createService(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(post("/services")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pintura Interior"))
                .andExpect(jsonPath("$.slug").value("pintura-interior"));
    }

    // ── PUT /services/{id} ────────────────────────────────────────────────────

    @Test
    void update_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/services/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_nonOwner_returns403() throws Exception {
        mockMvc.perform(put("/services/{id}", UUID.randomUUID())
                        .with(authentication(nonOwnerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_unknownService_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalogService.updateService(eq(COMPANY_ID), eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Service", id));

        mockMvc.perform(put("/services/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var request  = new UpdateServiceRequest("Pintura Exterior", null, null, null, null, null);
        var response = serviceResponse("Pintura Exterior");

        when(catalogService.updateService(eq(COMPANY_ID), eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/services/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pintura Exterior"));
    }

    // ── DELETE /services/{id} ─────────────────────────────────────────────────

    @Test
    void delete_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/services/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_nonOwner_returns403() throws Exception {
        mockMvc.perform(delete("/services/{id}", UUID.randomUUID())
                        .with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_unknownService_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Service", id))
                .when(catalogService).deleteService(COMPANY_ID, id);

        mockMvc.perform(delete("/services/{id}", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingService_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/services/{id}", id)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteService(COMPANY_ID, id);
    }

    // ── PATCH /services/{id}/reorder ──────────────────────────────────────────

    @Test
    void reorder_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/services/{id}/reorder", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderRequest(2))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reorder_nullDisplayOrder_returns400() throws Exception {
        mockMvc.perform(patch("/services/{id}/reorder", UUID.randomUUID())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorder_negativeOrder_returns400() throws Exception {
        mockMvc.perform(patch("/services/{id}/reorder", UUID.randomUUID())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderRequest(-1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorder_validOrder_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var response = serviceResponse("Pintura");

        when(catalogService.reorder(COMPANY_ID, id, 3)).thenReturn(response);

        mockMvc.perform(patch("/services/{id}/reorder", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pintura"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static ServiceResponse serviceResponse(String name) {
        return new ServiceResponse(
                UUID.randomUUID(), COMPANY_ID,
                name, name.toLowerCase().replace(" ", "-"),
                "Short desc", null, null,
                0, true,
                Instant.now(), Instant.now()
        );
    }
}
