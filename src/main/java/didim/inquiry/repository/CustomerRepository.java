package didim.inquiry.repository;

import didim.inquiry.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 기본적인 findAll, findById 등 JpaRepository에서 제공

    Customer findByCode(String code);

    boolean existsByCodeAndStatus(String customerCode , String status);

    List<Customer> findAllByStatus(String active);
    
    List<Customer> findByCodeInAndStatus(List<String> codes, String status);
}