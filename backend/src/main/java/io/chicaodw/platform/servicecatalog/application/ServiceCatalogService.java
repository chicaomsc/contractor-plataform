package io.chicaodw.platform.servicecatalog.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.dto.PublicServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.ServiceResponse;
import io.chicaodw.platform.servicecatalog.api.dto.UpdateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.mapper.ServiceMapper;
import io.chicaodw.platform.servicecatalog.domain.Service;
import io.chicaodw.platform.servicecatalog.infrastructure.persistence.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@Transactional
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository    serviceRepository;
    private final ServiceSlugGenerator slugGenerator;
    private final ServiceMapper        serviceMapper;

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ServiceResponse> listServices(UUID companyId) {
        return serviceRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId)
                                .stream().map(serviceMapper::toResponse).toList();
    }

    public ServiceResponse createService(UUID companyId, CreateServiceRequest request) {
        var service = new Service();
        service.setCompanyId(companyId);
        service.setName(request.name());
        service.setSlug(slugGenerator.generate(request.name(), companyId));
        service.setShortDescription(request.shortDescription());
        service.setDescription(request.description());
        service.setIcon(request.icon());
        service.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        service.setActive(request.active() == null || request.active());
        return serviceMapper.toResponse(serviceRepository.save(service));
    }

    public ServiceResponse updateService(UUID companyId, UUID id, UpdateServiceRequest request) {
        var service = findByIdAndCompany(id, companyId);

        if (request.name()             != null) service.setName(request.name());
        if (request.shortDescription() != null) service.setShortDescription(request.shortDescription());
        if (request.description()      != null) service.setDescription(request.description());
        if (request.icon()             != null) service.setIcon(request.icon());
        if (request.displayOrder()     != null) service.setDisplayOrder(request.displayOrder());
        if (request.active()           != null) service.setActive(request.active());

        return serviceMapper.toResponse(serviceRepository.save(service));
    }

    public void deleteService(UUID companyId, UUID id) {
        serviceRepository.delete(findByIdAndCompany(id, companyId));
    }

    public ServiceResponse reorder(UUID companyId, UUID id, int displayOrder) {
        var service = findByIdAndCompany(id, companyId);
        service.setDisplayOrder(displayOrder);
        return serviceMapper.toResponse(serviceRepository.save(service));
    }

    // ── Public (no authentication) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PublicServiceResponse> listPublicServices(UUID companyId) {
        return serviceRepository.findByCompanyIdAndActiveTrueOrderByDisplayOrderAsc(companyId)
                                .stream().map(serviceMapper::toPublicResponse).toList();
    }

    /** Used by other modules (e.g. estimate) to validate an optional catalogue reference without exposing the entity. */
    @Transactional(readOnly = true)
    public boolean existsForCompany(UUID companyId, UUID id) {
        return serviceRepository.existsByIdAndCompanyId(id, companyId);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private Service findByIdAndCompany(UUID id, UUID companyId) {
        return serviceRepository.findByIdAndCompanyId(id, companyId)
                                .orElseThrow(() -> new ResourceNotFoundException("Service", id));
    }
}
