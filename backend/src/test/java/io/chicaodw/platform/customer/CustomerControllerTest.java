package io.chicaodw.platform.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractControllerTest;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /customers ───────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_nonOwner_returns403() throws Exception {
        mockMvc.perform(get("/customers").with(authentication(nonOwnerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_authenticated_returns200WithItems() throws Exception {
        when(customerService.listCustomers(COMPANY_ID)).thenReturn(List.of(customerResponse("Jane Doe")));

        mockMvc.perform(get("/customers").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jane Doe"));
    }

    // ── GET /customers/{id} ──────────────────────────────────────────────────

    @Test
    void get_unknownCustomer_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.getCustomer(COMPANY_ID, id)).thenThrow(new ResourceNotFoundException("Customer", id));

        mockMvc.perform(get("/customers/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isNotFound());
    }

    // ── POST /customers ──────────────────────────────────────────────────────

    @Test
    void create_blankName_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(
                new CreateCustomerRequest("", "jane@example.com", null, null, null, null));

        mockMvc.perform(post("/customers")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void create_missingContactInfo_returns422() throws Exception {
        var request = new CreateCustomerRequest("Jane Doe", null, null, null, null, null);
        when(customerService.createCustomer(eq(COMPANY_ID), any()))
                .thenThrow(new BusinessRuleException("Customer must have at least one of email or phone"));

        mockMvc.perform(post("/customers")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        var request  = new CreateCustomerRequest("Jane Doe", "jane@example.com", "912345678", null, null, null);
        var response = customerResponse("Jane Doe");

        when(customerService.createCustomer(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(post("/customers")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    // ── PUT /customers/{id} ──────────────────────────────────────────────────

    @Test
    void update_unknownCustomer_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.updateCustomer(eq(COMPANY_ID), eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Customer", id));

        mockMvc.perform(put("/customers/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID id      = UUID.randomUUID();
        var request  = new UpdateCustomerRequest("Jane Updated", null, null, null, null, null, null);
        var response = customerResponse("Jane Updated");

        when(customerService.updateCustomer(eq(COMPANY_ID), eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/customers/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Updated"));
    }

    // ── DELETE /customers/{id} ───────────────────────────────────────────────

    @Test
    void delete_existingCustomer_returns204_andDeactivates() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/customers/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isNoContent());

        verify(customerService).deactivateCustomer(COMPANY_ID, id);
    }

    @Test
    void delete_unknownCustomer_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Customer", id)).when(customerService).deactivateCustomer(COMPANY_ID, id);

        mockMvc.perform(delete("/customers/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CustomerResponse customerResponse(String name) {
        return new CustomerResponse(
                UUID.randomUUID(), COMPANY_ID, name, "jane@example.com", "912345678", null, null, null,
                true, Instant.now(), Instant.now());
    }
}
