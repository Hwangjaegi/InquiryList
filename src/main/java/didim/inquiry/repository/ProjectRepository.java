package didim.inquiry.repository;

import didim.inquiry.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project , Long> {

//    List<Project> findByUserId(Long userId);
    //생성일기준 내림차순정렬
//    Page<Project> findByUserIdAndSubjectContainingOrderByCreatedAtDesc(Long userId , String search , Pageable pageable);
//    Page<Project> findByUserIdOrderByCreatedAtDesc(Long userId , Pageable pageable);

    //페이징 없이 다른곳에서 쓰일때 사용

    Page<Project> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Project> findByCustomerIdAndSubjectContainingOrderByCreatedAtDesc(Long customerId, String search, Pageable pageable);
    Page<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Project> findAllBySubjectContainingOrderByCreatedAtDesc(String search, Pageable pageable);
    Optional<Project> findBySubject(String subject);

    @Query("SELECT p FROM Project p LEFT JOIN p.customer c WHERE LOWER(p.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Project> findBySubjectOrCustomerCodeContainingIgnoreCase(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Project p")
    long countAllProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.createdAt >= :startOfMonth")
    long countNewProjectsThisMonth(@org.springframework.data.repository.query.Param("startOfMonth") java.time.LocalDateTime startOfMonth);
}
