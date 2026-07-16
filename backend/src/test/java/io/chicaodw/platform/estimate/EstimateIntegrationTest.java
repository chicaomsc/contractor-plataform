package io.chicaodw.platform.estimate;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.api.dto.MaterialRequest;
import io.chicaodw.platform.estimate.api.dto.UpdateEstimateRequest;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Full-stack flow against a real PostgreSQL container — exercises the V8 migration end to end. */
class EstimateIntegrationTest extends AbstractIntegrationTest {

    @Autowired EstimateService    estimateService;
    @Autowired EstimateRepository estimateRepository;
    @Autowired CustomerService    customerService;
    @Autowired CompanyRepository  companyRepository;
    @Autowired SettingsRepository settingsRepository;

    private UUID companyId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        var company = new Company();
        company.setName("Estimate Test Co");
        company.setSlug("estimate-test-" + UUID.randomUUID());
        company.setEmail("estimate-test@example.com");
        company.setCountry("PT");
        companyId = companyRepository.save(company).getId();

        var settings = new Settings();
        settings.setCompanyId(companyId);
        settings.setDefaultCurrency("EUR");
        settings.setDefaultTaxRate(new BigDecimal("23.00"));
        settings.setUpfrontPercentage(new BigDecimal("50.00"));
        settings.setEstimateValidityDays(30);
        settingsRepository.save(settings);

        customerId = customerService.createCustomer(companyId, new CreateCustomerRequest(
                "Jane Doe", "jane@example.com", null, null, null, null)).id();
    }

    @Test
    void createEstimate_persistsSnapshotsAndCalculatedTotals() {
        var response = estimateService.createEstimate(companyId, createRequest());

        assertThat(response.number()).startsWith("ORC-");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.vatRate()).isEqualByComparingTo("23.00");
        assertThat(response.laborSubtotal()).isEqualByComparingTo("200.00");
        assertThat(response.materialSubtotal()).isEqualByComparingTo("50.00");
        assertThat(response.subtotal()).isEqualByComparingTo("250.00");
        assertThat(response.vatAmount()).isEqualByComparingTo("57.50");
        assertThat(response.total()).isEqualByComparingTo("307.50");
        assertThat(response.upfrontAmount()).isEqualByComparingTo("153.75");
        assertThat(response.remainingAmount()).isEqualByComparingTo("153.75");
        assertThat(response.items()).hasSize(1);
        assertThat(response.materials()).hasSize(1);

        // number is unique per company
        var second = estimateService.createEstimate(companyId, createRequest());
        assertThat(second.number()).isNotEqualTo(response.number());
    }

    @Test
    void changingSettingsAfterCreation_doesNotAffectExistingEstimate() {
        var created = estimateService.createEstimate(companyId, createRequest());

        var settings = settingsRepository.findByCompanyId(companyId).orElseThrow();
        settings.setDefaultTaxRate(new BigDecimal("6.00"));
        settingsRepository.save(settings);

        var reloaded = estimateService.getEstimate(companyId, created.id());
        assertThat(reloaded.vatRate()).isEqualByComparingTo("23.00");
    }

    @Test
    void fullLifecycle_draftEditThenSendApproveComplete() {
        var created = estimateService.createEstimate(companyId, createRequest());

        var updated = estimateService.updateEstimate(companyId, created.id(), new UpdateEstimateRequest(
                null, "Updated title", null, null, null, null, null, null, null, null, null, null));
        assertThat(updated.title()).isEqualTo("Updated title");

        var sent = estimateService.changeStatus(companyId, created.id(), new ChangeEstimateStatusRequest(EstimateStatus.SENT));
        assertThat(sent.status()).isEqualTo(EstimateStatus.SENT);

        assertThatThrownBy(() -> estimateService.updateEstimate(companyId, created.id(), new UpdateEstimateRequest(
                null, "Should fail", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);

        var approved = estimateService.changeStatus(companyId, created.id(), new ChangeEstimateStatusRequest(EstimateStatus.APPROVED));
        assertThat(approved.status()).isEqualTo(EstimateStatus.APPROVED);

        var completed = estimateService.changeStatus(companyId, created.id(), new ChangeEstimateStatusRequest(EstimateStatus.COMPLETED));
        assertThat(completed.status()).isEqualTo(EstimateStatus.COMPLETED);

        assertThatThrownBy(() -> estimateService.changeStatus(companyId, created.id(),
                new ChangeEstimateStatusRequest(EstimateStatus.DRAFT)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteEstimate_onlyAllowedInDraft() {
        var created = estimateService.createEstimate(companyId, createRequest());
        estimateService.changeStatus(companyId, created.id(), new ChangeEstimateStatusRequest(EstimateStatus.SENT));

        assertThatThrownBy(() -> estimateService.deleteEstimate(companyId, created.id()))
                .isInstanceOf(BusinessRuleException.class);

        var draft = estimateService.createEstimate(companyId, createRequest());
        estimateService.deleteEstimate(companyId, draft.id());

        assertThat(estimateRepository.findByIdAndCompanyId(draft.id(), companyId)).isEmpty();
    }

    @Test
    void inactiveCustomer_cannotBeAssignedToNewEstimate() {
        customerService.deactivateCustomer(companyId, customerId);

        assertThatThrownBy(() -> estimateService.createEstimate(companyId, createRequest()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listEstimates_filtersByStatusAndCustomer() {
        var draft = estimateService.createEstimate(companyId, createRequest());
        var sent = estimateService.createEstimate(companyId, createRequest());
        estimateService.changeStatus(companyId, sent.id(), new ChangeEstimateStatusRequest(EstimateStatus.SENT));

        assertThat(estimateService.listEstimates(companyId, EstimateStatus.DRAFT, null))
                .extracting("id").containsExactly(draft.id());
        assertThat(estimateService.listEstimates(companyId, EstimateStatus.SENT, null))
                .extracting("id").containsExactly(sent.id());
        assertThat(estimateService.listEstimates(companyId, null, customerId))
                .extracting("id").containsExactlyInAnyOrder(draft.id(), sent.id());
    }

    @Test
    void createEstimate_unknownServiceId_throwsResourceNotFoundException() {
        var item = new EstimateItemRequest(UUID.randomUUID(), "Labor", BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("10"), null);
        var request = new CreateEstimateRequest(customerId, "Painting", null, null, null, null, null, null,
                null, null, List.of(item), List.of());

        assertThatThrownBy(() -> estimateService.createEstimate(companyId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateEstimateRequest createRequest() {
        var item = new EstimateItemRequest(null, "Wall painting", new BigDecimal("2"), EstimateUnit.M2, new BigDecimal("100.00"), null);
        var material = new MaterialRequest("Paint", null, BigDecimal.ONE, EstimateUnit.UNIT, new BigDecimal("50.00"), null);
        return new CreateEstimateRequest(customerId, "Painting job", "Full house", null, null, null, null, null,
                null, null, List.of(item), List.of(material));
    }
}
