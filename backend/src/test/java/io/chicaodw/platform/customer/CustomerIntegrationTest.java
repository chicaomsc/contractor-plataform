package io.chicaodw.platform.customer;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerAddressRequest;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.customer.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Full-stack CRUD flow against a real PostgreSQL container — exercises the V7 migration end to end. */
class CustomerIntegrationTest extends AbstractIntegrationTest {

    @Autowired CustomerService    customerService;
    @Autowired CustomerRepository customerRepository;
    @Autowired CompanyRepository  companyRepository;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        var company = new Company();
        company.setName("Integration Test Co");
        company.setSlug("integration-test-" + UUID.randomUUID());
        company.setEmail("integration@example.com");
        company.setCountry("PT");
        companyId = companyRepository.save(company).getId();
    }

    @Test
    void createGetListUpdateDeactivate_fullLifecycle() {
        var created = customerService.createCustomer(companyId, new CreateCustomerRequest(
                "Jane Doe", "jane@example.com", "912345678", "PT123456789",
                new CustomerAddressRequest("Rua A", "Lisboa", "1000-001", null, "PT"),
                "VIP client"));

        assertThat(created.active()).isTrue();
        assertThat(created.address().city()).isEqualTo("Lisboa");

        var fetched = customerService.getCustomer(companyId, created.id());
        assertThat(fetched.name()).isEqualTo("Jane Doe");

        assertThat(customerService.listCustomers(companyId)).extracting("id").containsExactly(created.id());

        var updated = customerService.updateCustomer(companyId, created.id(),
                new UpdateCustomerRequest(null, null, "999999999", null, null, null, null));
        assertThat(updated.phone()).isEqualTo("999999999");
        assertThat(updated.email()).isEqualTo("jane@example.com"); // unchanged

        customerService.deactivateCustomer(companyId, created.id());
        var deactivated = customerRepository.findById(created.id()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
        // Soft delete: the row must still exist for estimate history integrity.
        assertThat(customerRepository.findById(created.id())).isPresent();
    }

    @Test
    void taxNumber_isNotUniqueAcrossCustomers() {
        var first = customerService.createCustomer(companyId, new CreateCustomerRequest(
                "Customer A", "a@example.com", null, "SAME-TAX-NUMBER", null, null));
        var second = customerService.createCustomer(companyId, new CreateCustomerRequest(
                "Customer B", "b@example.com", null, "SAME-TAX-NUMBER", null, null));

        assertThat(first.taxNumber()).isEqualTo(second.taxNumber());
    }

    @Test
    void createCustomer_withoutEmailOrPhone_isRejected() {
        assertThatThrownBy(() -> customerService.createCustomer(companyId,
                new CreateCustomerRequest("No Contact", null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
