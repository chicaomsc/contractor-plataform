package io.chicaodw.platform.customer.application;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.api.mapper.CustomerMapper;
import io.chicaodw.platform.customer.domain.Customer;
import io.chicaodw.platform.customer.infrastructure.persistence.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer is intentionally referenced by other modules (estimate) only through this
 * application service — never through {@link CustomerRepository} directly — so ownership
 * and active-state rules stay centralised here.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper     customerMapper;

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers(UUID companyId) {
        return customerRepository.findByCompanyIdOrderByNameAsc(companyId)
                .stream().map(customerMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID companyId, UUID id) {
        return customerMapper.toResponse(findByIdAndCompany(id, companyId));
    }

    public CustomerResponse createCustomer(UUID companyId, CreateCustomerRequest request) {
        requireContactInfo(request.email(), request.phone());

        var customer = new Customer();
        customer.setCompanyId(companyId);
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setTaxNumber(request.taxNumber());
        customer.setAddress(toAddress(request.address()));
        customer.setNotes(request.notes());
        customer.setActive(true);

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    public CustomerResponse updateCustomer(UUID companyId, UUID id, UpdateCustomerRequest request) {
        var customer = findByIdAndCompany(id, companyId);

        if (request.name()      != null) customer.setName(request.name());
        if (request.email()     != null) customer.setEmail(request.email());
        if (request.phone()     != null) customer.setPhone(request.phone());
        if (request.taxNumber() != null) customer.setTaxNumber(request.taxNumber());
        if (request.notes()     != null) customer.setNotes(request.notes());
        if (request.active()    != null) customer.setActive(request.active());

        if (request.address() != null) {
            var addr    = request.address();
            var current = customer.getAddress() != null ? customer.getAddress() : Address.builder().build();
            customer.setAddress(Address.builder()
                    .street(addr.street()         != null ? addr.street()     : current.getStreet())
                    .city(addr.city()             != null ? addr.city()       : current.getCity())
                    .postalCode(addr.postalCode() != null ? addr.postalCode() : current.getPostalCode())
                    .region(addr.region()         != null ? addr.region()     : current.getRegion())
                    .country(addr.country()       != null ? addr.country()    : current.getCountry())
                    .build());
        }

        requireContactInfo(customer.getEmail(), customer.getPhone());

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    /** Soft delete: customers are kept for historical integrity — estimates reference them permanently. */
    public void deactivateCustomer(UUID companyId, UUID id) {
        var customer = findByIdAndCompany(id, companyId);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    /**
     * Validates that a customer can be assigned to an estimate: must exist within the
     * caller's company and must be active. Used by the estimate module.
     */
    @Transactional(readOnly = true)
    public void assertAssignable(UUID companyId, UUID customerId) {
        var customer = findByIdAndCompany(customerId, companyId);
        if (!customer.isActive()) {
            throw new BusinessRuleException("Customer is inactive and cannot be assigned to an estimate: " + customerId);
        }
    }

    private void requireContactInfo(String email, String phone) {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        if (!hasEmail && !hasPhone) {
            throw new BusinessRuleException("Customer must have at least one of email or phone");
        }
    }

    private static Address toAddress(io.chicaodw.platform.customer.api.dto.CustomerAddressRequest address) {
        if (address == null) return null;
        return Address.builder()
                .street(address.street())
                .city(address.city())
                .postalCode(address.postalCode())
                .region(address.region())
                .country(address.country())
                .build();
    }

    private Customer findByIdAndCompany(UUID id, UUID companyId) {
        return customerRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
