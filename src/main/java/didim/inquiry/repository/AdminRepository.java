package didim.inquiry.repository;

import didim.inquiry.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminRepository extends JpaRepository<Customer , Long> {
    //jpa가 code로 중복 체크 자동화
    boolean existsByCode(String code);

    boolean existsByCodeAndStatus(String code, String status);

    Page<Customer> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Customer> findByCodeContainingIgnoreCaseOrCompanyContainingIgnoreCase(String search, String search2 , Pageable pageable);

    long countByStatusIgnoreCase(String active);

    long countByCreatedAtBetween(LocalDateTime startOfMonth, LocalDateTime endOfMonth);


}
