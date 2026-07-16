package io.chicaodw.platform.estimate;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.api.dto.MaterialRequest;
import io.chicaodw.platform.estimate.api.dto.UpdateEstimateRequest;
import io.chicaodw.platform.estimate.api.mapper.EstimateMapper;
import io.chicaodw.platform.estimate.api.mapper.EstimateMapperImpl;
import io.chicaodw.platform.estimate.application.EstimateNumberGenerator;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {

    @Mock EstimateRepository      estimateRepository;
    @Mock SettingsRepository      settingsRepository;
    @Mock EstimateNumberGenerator numberGenerator;
    @Mock CustomerService         customerService;
    @Mock ServiceCatalogService   serviceCatalogService;

    final EstimateMapper estimateMapper = new EstimateMapperImpl();

    EstimateService estimateService;

    private UUID companyId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        estimateService = new EstimateService(
                estimateRepository, settingsRepository, numberGenerator,
                customerService, serviceCatalogService, estimateMapper);
    }

    // ── createEstimate ───────────────────────────────────────────────────────

    @Test
    void createEstimate_snapshotsSettingsDefaults_whenNotOverridden() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(defaultSettings()));
        when(numberGenerator.generate(companyId)).thenReturn("ORC-2026-0001");
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = createRequest(null, null, List.of(itemRequest("100.00")), List.of());
        var response = estimateService.createEstimate(companyId, request);

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.vatRate()).isEqualByComparingTo("23.00");
        assertThat(response.upfrontPercentage()).isEqualByComparingTo("50.00");
        assertThat(response.number()).isEqualTo("ORC-2026-0001");
        assertThat(response.status()).isEqualTo(EstimateStatus.DRAFT);
    }

    @Test
    void createEstimate_clientOverrides_vatAndUpfront_areRespected() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(defaultSettings()));
        when(numberGenerator.generate(companyId)).thenReturn("ORC-2026-0001");
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = createRequest(new BigDecimal("0"), new BigDecimal("100"), List.of(itemRequest("200.00")), List.of());
        var response = estimateService.createEstimate(companyId, request);

        assertThat(response.vatRate()).isEqualByComparingTo("0");
        assertThat(response.upfrontPercentage()).isEqualByComparingTo("100");
        assertThat(response.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void createEstimate_computesTotalsFromItemsAndMaterials() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(defaultSettings()));
        when(numberGenerator.generate(companyId)).thenReturn("ORC-2026-0001");
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = createRequest(null, null,
                List.of(itemRequest("100.00")), List.of(materialRequest("50.00")));
        var response = estimateService.createEstimate(companyId, request);

        assertThat(response.laborSubtotal()).isEqualByComparingTo("100.00");
        assertThat(response.materialSubtotal()).isEqualByComparingTo("50.00");
        assertThat(response.subtotal()).isEqualByComparingTo("150.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.materials()).hasSize(1);
    }

    @Test
    void createEstimate_inactiveCustomer_propagatesBusinessRuleException() {
        doThrow(new BusinessRuleException("Customer is inactive"))
                .when(customerService).assertAssignable(companyId, customerId);

        var request = createRequest(null, null, List.of(), List.of());

        assertThatThrownBy(() -> estimateService.createEstimate(companyId, request))
                .isInstanceOf(BusinessRuleException.class);
        verify(estimateRepository, never()).save(any());
    }

    @Test
    void createEstimate_crossTenantCustomer_propagatesResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("Customer", customerId))
                .when(customerService).assertAssignable(companyId, customerId);

        var request = createRequest(null, null, List.of(), List.of());

        assertThatThrownBy(() -> estimateService.createEstimate(companyId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEstimate_serviceIdFromAnotherCompany_throwsResourceNotFoundException() {
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(defaultSettings()));
        when(numberGenerator.generate(companyId)).thenReturn("ORC-2026-0001");
        UUID foreignServiceId = UUID.randomUUID();
        when(serviceCatalogService.existsForCompany(companyId, foreignServiceId)).thenReturn(false);

        var item = new EstimateItemRequest(foreignServiceId, "Item", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("10"), null);
        var request = createRequest(null, null, List.of(item), List.of());

        assertThatThrownBy(() -> estimateService.createEstimate(companyId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEstimate_validUntilDefaultsFromSettingsValidityDays() {
        var settings = defaultSettings();
        settings.setEstimateValidityDays(15);
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));
        when(numberGenerator.generate(companyId)).thenReturn("ORC-2026-0001");
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = createRequest(null, null, List.of(), List.of());
        var response = estimateService.createEstimate(companyId, request);

        assertThat(response.validUntil()).isEqualTo(response.issueDate().plusDays(15));
    }

    // ── updateEstimate ───────────────────────────────────────────────────────

    @Test
    void updateEstimate_draftEstimate_recalculatesTotals() {
        var estimate = draftEstimate();
        when(estimateRepository.findDetailByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateEstimateRequest(null, "New title", null, null, null, null, null, null, null, null,
                List.of(itemRequest("300.00")), null);
        var response = estimateService.updateEstimate(companyId, estimate.getId(), request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.laborSubtotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void updateEstimate_nonDraftEstimate_throwsBusinessRuleException() {
        var estimate = draftEstimate();
        estimate.setStatus(EstimateStatus.SENT);
        when(estimateRepository.findDetailByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));

        var request = new UpdateEstimateRequest(null, "New title", null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> estimateService.updateEstimate(companyId, estimate.getId(), request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateEstimate_unknownEstimate_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(estimateRepository.findDetailByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

        var request = new UpdateEstimateRequest(null, "New title", null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> estimateService.updateEstimate(companyId, id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteEstimate ───────────────────────────────────────────────────────

    @Test
    void deleteEstimate_draft_deletes() {
        var estimate = draftEstimate();
        when(estimateRepository.findByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));

        estimateService.deleteEstimate(companyId, estimate.getId());

        verify(estimateRepository).delete(estimate);
    }

    @Test
    void deleteEstimate_nonDraft_throwsBusinessRuleException() {
        var estimate = draftEstimate();
        estimate.setStatus(EstimateStatus.APPROVED);
        when(estimateRepository.findByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));

        assertThatThrownBy(() -> estimateService.deleteEstimate(companyId, estimate.getId()))
                .isInstanceOf(BusinessRuleException.class);
        verify(estimateRepository, never()).delete(any(Estimate.class));
    }

    // ── changeStatus ─────────────────────────────────────────────────────────

    @Test
    void changeStatus_validTransition_updatesStatus() {
        var estimate = draftEstimate();
        when(estimateRepository.findDetailByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = estimateService.changeStatus(companyId, estimate.getId(), new ChangeEstimateStatusRequest(EstimateStatus.SENT));

        assertThat(response.status()).isEqualTo(EstimateStatus.SENT);
    }

    @Test
    void changeStatus_invalidTransition_throwsConflictException() {
        var estimate = draftEstimate();
        when(estimateRepository.findDetailByIdAndCompanyId(estimate.getId(), companyId)).thenReturn(Optional.of(estimate));

        assertThatThrownBy(() -> estimateService.changeStatus(
                companyId, estimate.getId(), new ChangeEstimateStatusRequest(EstimateStatus.APPROVED)))
                .isInstanceOf(ConflictException.class);
        verify(estimateRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateEstimateRequest createRequest(
            BigDecimal vatRate, BigDecimal upfrontPercentage,
            List<EstimateItemRequest> items, List<MaterialRequest> materials) {
        return new CreateEstimateRequest(
                customerId, "Painting job", "Full house", null, null, null, null, null,
                vatRate, upfrontPercentage, items, materials);
    }

    private EstimateItemRequest itemRequest(String unitPrice) {
        return new EstimateItemRequest(null, "Labor", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal(unitPrice), null);
    }

    private MaterialRequest materialRequest(String unitPrice) {
        return new MaterialRequest("Paint", null, BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal(unitPrice), null);
    }

    private Settings defaultSettings() {
        var settings = new Settings();
        settings.setCompanyId(companyId);
        settings.setDefaultCurrency("EUR");
        settings.setDefaultTaxRate(new BigDecimal("23.00"));
        settings.setUpfrontPercentage(new BigDecimal("50.00"));
        settings.setEstimateValidityDays(30);
        return settings;
    }

    private Estimate draftEstimate() {
        var estimate = new Estimate();
        estimate.setCompanyId(companyId);
        estimate.setCustomerId(customerId);
        estimate.setNumber("ORC-2026-0001");
        estimate.setTitle("Painting job");
        estimate.setStatus(EstimateStatus.DRAFT);
        estimate.setIssueDate(java.time.LocalDate.now());
        estimate.setCurrency("EUR");
        estimate.setVatRate(new BigDecimal("23.00"));
        estimate.setUpfrontPercentage(new BigDecimal("50.00"));
        ReflectionTestUtils.setField(estimate, "id", UUID.randomUUID());
        return estimate;
    }
}
