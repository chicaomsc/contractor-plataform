package io.chicaodw.platform.customer;

import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.api.mapper.CustomerMapper;
import io.chicaodw.platform.customer.api.mapper.CustomerMapperImpl;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.customer.domain.Customer;
import io.chicaodw.platform.customer.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;

    final CustomerMapper customerMapper = new CustomerMapperImpl();

    CustomerService customerService;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        customerService = new CustomerService(customerRepository, customerMapper);
    }

    // ── createCustomer ───────────────────────────────────────────────────────

    @Test
    void createCustomer_withEmailOnly_succeeds() {
        var request = new CreateCustomerRequest("Jane Doe", "jane@example.com", null, null, null, null);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = customerService.createCustomer(companyId, request);

        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createCustomer_withPhoneOnly_succeeds() {
        var request = new CreateCustomerRequest("Jane Doe", null, "912345678", null, null, null);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = customerService.createCustomer(companyId, request);

        assertThat(response.phone()).isEqualTo("912345678");
    }

    @Test
    void createCustomer_withoutEmailOrPhone_throwsBusinessRuleException() {
        var request = new CreateCustomerRequest("Jane Doe", null, null, null, null, null);

        assertThatThrownBy(() -> customerService.createCustomer(companyId, request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createCustomer_persistsCompanyIdFromContext_notFromRequest() {
        var request = new CreateCustomerRequest("Jane Doe", "jane@example.com", null, null, null, null);
        var captor = ArgumentCaptor.forClass(Customer.class);
        when(customerRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        customerService.createCustomer(companyId, request);

        assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId);
    }

    // ── updateCustomer ───────────────────────────────────────────────────────

    @Test
    void updateCustomer_partial_onlyChangesProvidedFields() {
        var customer = existingCustomer();
        when(customerRepository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateCustomerRequest(null, null, "999888777", null, null, null, null);
        var response = customerService.updateCustomer(companyId, customer.getId(), request);

        assertThat(response.phone()).isEqualTo("999888777");
        assertThat(response.name()).isEqualTo("Original Name");
    }

    @Test
    void updateCustomer_clearingBothEmailAndPhone_throwsBusinessRuleException() {
        var customer = existingCustomer();
        customer.setPhone(null); // only email remains
        when(customerRepository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(customer));

        var request = new UpdateCustomerRequest(null, "", null, null, null, null, null);

        assertThatThrownBy(() -> customerService.updateCustomer(companyId, customer.getId(), request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateCustomer_crossTenant_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(companyId, id,
                new UpdateCustomerRequest("New name", null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deactivateCustomer ───────────────────────────────────────────────────

    @Test
    void deactivateCustomer_setsActiveFalse_doesNotDelete() {
        var customer = existingCustomer();
        when(customerRepository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        customerService.deactivateCustomer(companyId, customer.getId());

        assertThat(customer.isActive()).isFalse();
    }

    // ── assertAssignable ─────────────────────────────────────────────────────

    @Test
    void assertAssignable_activeCustomer_doesNotThrow() {
        var customer = existingCustomer();
        when(customerRepository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(customer));

        customerService.assertAssignable(companyId, customer.getId());
    }

    @Test
    void assertAssignable_inactiveCustomer_throwsBusinessRuleException() {
        var customer = existingCustomer();
        customer.setActive(false);
        when(customerRepository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.assertAssignable(companyId, customer.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assertAssignable_unknownCustomer_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.assertAssignable(companyId, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Customer existingCustomer() {
        var customer = new Customer();
        customer.setCompanyId(companyId);
        customer.setName("Original Name");
        customer.setEmail("original@example.com");
        customer.setPhone("911111111");
        customer.setActive(true);
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        return customer;
    }
}
