package io.chicaodw.platform.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chicaodw.platform.AbstractControllerTest;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateResponse;
import io.chicaodw.platform.estimate.api.dto.EstimateSummaryResponse;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstimateControllerTest extends AbstractControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── GET /estimates ───────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/estimates")).andExpect(status().isUnauthorized());
    }

    @Test
    void list_withStatusFilter_returns200() throws Exception {
        when(estimateService.listEstimates(COMPANY_ID, EstimateStatus.DRAFT, null))
                .thenReturn(List.of(summary("ORC-2026-0001")));

        mockMvc.perform(get("/estimates?status=DRAFT").with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("ORC-2026-0001"));
    }

    // ── GET /estimates/{id} ──────────────────────────────────────────────────

    @Test
    void get_unknownEstimate_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(estimateService.getEstimate(COMPANY_ID, id)).thenThrow(new ResourceNotFoundException("Estimate", id));

        mockMvc.perform(get("/estimates/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isNotFound());
    }

    // ── POST /estimates ──────────────────────────────────────────────────────

    @Test
    void create_blankTitle_returns400() throws Exception {
        var request = new CreateEstimateRequest(UUID.randomUUID(), "", null, null, null, null, null, null,
                null, null, List.of(), List.of());

        mockMvc.perform(post("/estimates")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void create_inactiveCustomer_returns422() throws Exception {
        var request = new CreateEstimateRequest(UUID.randomUUID(), "Painting", null, null, null, null, null, null,
                null, null, List.of(), List.of());
        when(estimateService.createEstimate(eq(COMPANY_ID), any()))
                .thenThrow(new BusinessRuleException("Customer is inactive"));

        mockMvc.perform(post("/estimates")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_crossTenantCustomer_returns404() throws Exception {
        UUID customerId = UUID.randomUUID();
        var request = new CreateEstimateRequest(customerId, "Painting", null, null, null, null, null, null,
                null, null, List.of(), List.of());
        when(estimateService.createEstimate(eq(COMPANY_ID), any()))
                .thenThrow(new ResourceNotFoundException("Customer", customerId));

        mockMvc.perform(post("/estimates")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        var request  = new CreateEstimateRequest(UUID.randomUUID(), "Painting", null, null, null, null, null, null,
                null, null, List.of(), List.of());
        var response = detail("ORC-2026-0001", EstimateStatus.DRAFT);

        when(estimateService.createEstimate(eq(COMPANY_ID), any())).thenReturn(response);

        mockMvc.perform(post("/estimates")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("ORC-2026-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // ── PUT /estimates/{id} ──────────────────────────────────────────────────

    @Test
    void update_notEditable_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        when(estimateService.updateEstimate(eq(COMPANY_ID), eq(id), any()))
                .thenThrow(new BusinessRuleException("Estimate is not editable in status SENT"));

        mockMvc.perform(put("/estimates/{id}", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── DELETE /estimates/{id} ───────────────────────────────────────────────

    @Test
    void delete_nonDraft_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new BusinessRuleException("Only DRAFT estimates can be deleted"))
                .when(estimateService).deleteEstimate(COMPANY_ID, id);

        mockMvc.perform(delete("/estimates/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void delete_draftEstimate_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/estimates/{id}", id).with(authentication(ownerAuth())))
                .andExpect(status().isNoContent());
    }

    // ── PATCH /estimates/{id}/status ─────────────────────────────────────────

    @Test
    void changeStatus_invalidTransition_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(estimateService.changeStatus(eq(COMPANY_ID), eq(id), any()))
                .thenThrow(new ConflictException("Invalid status transition from DRAFT to APPROVED"));

        mockMvc.perform(patch("/estimates/{id}/status", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEstimateStatusRequest(EstimateStatus.APPROVED))))
                .andExpect(status().isConflict());
    }

    @Test
    void changeStatus_validTransition_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(estimateService.changeStatus(eq(COMPANY_ID), eq(id), any()))
                .thenReturn(detail("ORC-2026-0001", EstimateStatus.SENT));

        mockMvc.perform(patch("/estimates/{id}/status", id)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEstimateStatusRequest(EstimateStatus.SENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static EstimateSummaryResponse summary(String number) {
        return new EstimateSummaryResponse(
                UUID.randomUUID(), COMPANY_ID, UUID.randomUUID(), number, "Painting",
                EstimateStatus.DRAFT, LocalDate.now(), LocalDate.now().plusDays(30), "EUR",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, Instant.now(), Instant.now());
    }

    private static EstimateResponse detail(String number, EstimateStatus status) {
        return new EstimateResponse(
                UUID.randomUUID(), COMPANY_ID, UUID.randomUUID(), number, "Painting", null,
                status, LocalDate.now(), LocalDate.now().plusDays(30), null, null, null, null,
                "EUR", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), Instant.now(), Instant.now());
    }
}
