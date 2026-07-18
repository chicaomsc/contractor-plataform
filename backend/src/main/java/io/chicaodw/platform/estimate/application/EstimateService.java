package io.chicaodw.platform.estimate.application;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.customer.api.dto.CustomerAddressResponse;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateResponse;
import io.chicaodw.platform.estimate.api.dto.EstimateSummaryResponse;
import io.chicaodw.platform.estimate.api.dto.MaterialRequest;
import io.chicaodw.platform.estimate.api.dto.UpdateEstimateRequest;
import io.chicaodw.platform.estimate.api.mapper.EstimateMapper;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateCalculationService;
import io.chicaodw.platform.estimate.domain.EstimateItem;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateStatusTransitionService;
import io.chicaodw.platform.estimate.domain.Material;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class EstimateService {

    private final EstimateRepository        estimateRepository;
    private final SettingsRepository        settingsRepository;
    private final EstimateNumberGenerator   numberGenerator;
    private final CustomerService           customerService;
    private final ServiceCatalogService     serviceCatalogService;
    private final EstimateMapper            estimateMapper;

    private final EstimateCalculationService calculationService = new EstimateCalculationService();
    private final EstimateStatusTransitionService transitionService = new EstimateStatusTransitionService();

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EstimateSummaryResponse> listEstimates(UUID companyId, EstimateStatus status, UUID customerId) {
        return estimateRepository.findByFilters(companyId, status, customerId)
                .stream().map(estimateMapper::toSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public EstimateResponse getEstimate(UUID companyId, UUID id) {
        return estimateMapper.toResponse(findDetailByIdAndCompany(id, companyId));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public EstimateResponse createEstimate(UUID companyId, CreateEstimateRequest request) {
        var customer = customerService.getAssignableCustomer(companyId, request.customerId());
        var settings = settingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings", companyId));

        var estimate = new Estimate();
        estimate.setCompanyId(companyId);
        estimate.setCustomerId(request.customerId());
        applyCustomerSnapshot(estimate, customer);
        estimate.setNumber(numberGenerator.generate(companyId));
        estimate.setTitle(request.title());
        estimate.setDescription(request.description());
        estimate.setStatus(EstimateStatus.DRAFT);

        LocalDate issueDate = LocalDate.now();
        estimate.setIssueDate(issueDate);
        estimate.setValidUntil(request.validUntil() != null
                ? request.validUntil()
                : issueDate.plusDays(settings.getEstimateValidityDays()));
        estimate.setExpectedStartDate(request.expectedStartDate());
        estimate.setEstimatedDurationDays(request.estimatedDurationDays());
        estimate.setNotes(request.notes());
        estimate.setTerms(request.terms());

        // Snapshots — see ADR-007 / Estimate javadoc. Never re-read after this point.
        estimate.setCurrency(settings.getDefaultCurrency());
        estimate.setVatRate(request.vatRate() != null ? request.vatRate() : settings.getDefaultTaxRate());
        estimate.setUpfrontPercentage(request.upfrontPercentage() != null
                ? request.upfrontPercentage()
                : settings.getUpfrontPercentage());

        applyItems(estimate, companyId, request.items());
        applyMaterials(estimate, request.materials());
        recalculate(estimate);

        return estimateMapper.toResponse(estimateRepository.save(estimate));
    }

    public EstimateResponse updateEstimate(UUID companyId, UUID id, UpdateEstimateRequest request) {
        var estimate = findDetailByIdAndCompany(id, companyId);
        requireEditable(estimate);

        if (request.customerId() != null) {
            var customer = customerService.getAssignableCustomer(companyId, request.customerId());
            estimate.setCustomerId(request.customerId());
            applyCustomerSnapshot(estimate, customer);
        }
        if (request.title()                 != null) estimate.setTitle(request.title());
        if (request.description()           != null) estimate.setDescription(request.description());
        if (request.validUntil()            != null) estimate.setValidUntil(request.validUntil());
        if (request.expectedStartDate()     != null) estimate.setExpectedStartDate(request.expectedStartDate());
        if (request.estimatedDurationDays() != null) estimate.setEstimatedDurationDays(request.estimatedDurationDays());
        if (request.notes()                 != null) estimate.setNotes(request.notes());
        if (request.terms()                 != null) estimate.setTerms(request.terms());
        if (request.vatRate()               != null) estimate.setVatRate(request.vatRate());
        if (request.upfrontPercentage()     != null) estimate.setUpfrontPercentage(request.upfrontPercentage());

        if (request.items()     != null) applyItems(estimate, companyId, request.items());
        if (request.materials() != null) applyMaterials(estimate, request.materials());

        recalculate(estimate);
        return estimateMapper.toResponse(estimateRepository.save(estimate));
    }

    public void deleteEstimate(UUID companyId, UUID id) {
        var estimate = findByIdAndCompany(id, companyId);
        if (estimate.getStatus() != EstimateStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT estimates can be deleted — cancel it instead (status: " + estimate.getStatus() + ")");
        }
        estimateRepository.delete(estimate);
    }

    public EstimateResponse changeStatus(UUID companyId, UUID id, ChangeEstimateStatusRequest request) {
        var estimate = findDetailByIdAndCompany(id, companyId);
        transitionService.validateTransition(estimate.getStatus(), request.status());
        estimate.setStatus(request.status());
        return estimateMapper.toResponse(estimateRepository.save(estimate));
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Freezes the customer's current identity/contact data onto the Estimate. Called on
     * creation and whenever the assigned customer changes — never re-applied from a plain
     * Customer edit, so a PDF generated later still reflects the customer as they were when
     * assigned, not as they are now.
     */
    private void applyCustomerSnapshot(Estimate estimate, CustomerResponse customer) {
        estimate.setCustomerNameSnapshot(customer.name());
        estimate.setCustomerEmailSnapshot(customer.email());
        estimate.setCustomerPhoneSnapshot(customer.phone());
        estimate.setCustomerTaxNumberSnapshot(customer.taxNumber());
        estimate.setCustomerAddressSnapshot(toAddress(customer.address()));
    }

    private static Address toAddress(CustomerAddressResponse address) {
        if (address == null) return null;
        return Address.builder()
                .street(address.street())
                .city(address.city())
                .postalCode(address.postalCode())
                .region(address.region())
                .country(address.country())
                .build();
    }

    private void applyItems(Estimate estimate, UUID companyId, List<EstimateItemRequest> items) {
        estimate.clearItems();
        if (items == null) return;
        int order = 0;
        for (var req : items) {
            if (req.serviceId() != null && !serviceCatalogService.existsForCompany(companyId, req.serviceId())) {
                throw new ResourceNotFoundException("Service", req.serviceId());
            }
            var item = new EstimateItem();
            item.setServiceId(req.serviceId());
            item.setDescription(req.description());
            item.setQuantity(req.quantity());
            item.setUnit(req.unit());
            item.setUnitPrice(req.unitPrice());
            item.setTotal(calculationService.calculateLineTotal(req.quantity(), req.unitPrice()));
            item.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : order);
            estimate.addItem(item);
            order++;
        }
    }

    private void applyMaterials(Estimate estimate, List<MaterialRequest> materials) {
        estimate.clearMaterials();
        if (materials == null) return;
        int order = 0;
        for (var req : materials) {
            var material = new Material();
            material.setName(req.name());
            material.setDescription(req.description());
            material.setQuantity(req.quantity());
            material.setUnit(req.unit());
            material.setUnitPrice(req.unitPrice());
            material.setTotal(calculationService.calculateLineTotal(req.quantity(), req.unitPrice()));
            material.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : order);
            estimate.addMaterial(material);
            order++;
        }
    }

    private void recalculate(Estimate estimate) {
        var totals = calculationService.calculate(
                estimate.getItems(), estimate.getMaterials(), estimate.getVatRate(), estimate.getUpfrontPercentage());
        estimate.setLaborSubtotal(totals.laborSubtotal());
        estimate.setMaterialSubtotal(totals.materialSubtotal());
        estimate.setSubtotal(totals.subtotal());
        estimate.setVatAmount(totals.vatAmount());
        estimate.setTotal(totals.total());
        estimate.setUpfrontAmount(totals.upfrontAmount());
        estimate.setRemainingAmount(totals.remainingAmount());
    }

    private void requireEditable(Estimate estimate) {
        if (estimate.getStatus() != EstimateStatus.DRAFT) {
            throw new BusinessRuleException("Estimate is not editable in status " + estimate.getStatus());
        }
    }

    private Estimate findByIdAndCompany(UUID id, UUID companyId) {
        return estimateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", id));
    }

    private Estimate findDetailByIdAndCompany(UUID id, UUID companyId) {
        return estimateRepository.findDetailByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", id));
    }
}
