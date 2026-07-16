package io.chicaodw.platform.customer.api;

import io.chicaodw.platform.auth.infrastructure.security.JwtPrincipal;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.CustomerResponse;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Company customer portfolio — admin endpoints")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List all customers for the authenticated company")
    public List<CustomerResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return customerService.listCustomers(principal.companyId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer by id")
    public CustomerResponse get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        return customerService.getCustomer(principal.companyId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new customer")
    public CustomerResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(principal.companyId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a customer (partial update — null fields are ignored)")
    public CustomerResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.updateCustomer(principal.companyId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a customer (soft delete — customers referenced by estimates are never removed)")
    public void delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        customerService.deactivateCustomer(principal.companyId(), id);
    }
}
