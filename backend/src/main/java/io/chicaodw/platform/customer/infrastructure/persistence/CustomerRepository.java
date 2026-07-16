package io.chicaodw.platform.customer.infrastructure.persistence;

import io.chicaodw.platform.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<Customer> findByIdAndCompanyId(UUID id, UUID companyId);
}
