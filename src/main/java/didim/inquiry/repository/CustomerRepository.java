package didim.inquiry.repository;

import didim.inquiry.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 기본적인 findAll, findById 등 JpaRepository에서 제공

    Customer findByCode(String code);
}