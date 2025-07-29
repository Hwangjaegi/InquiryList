package didim.inquiry.repository;

import didim.inquiry.domain.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRepository extends JpaRepository<Manager , Long> {
    boolean existsByEmail(String email);

    List<Manager> findAllByUserId(Long userId);
}
