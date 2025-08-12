package didim.inquiry.repository;

import didim.inquiry.domain.Manager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRepository extends JpaRepository<Manager , Long> {
    boolean existsByEmail(String email);

    List<Manager> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 특정 사용자 ID로 매니저 페이징 조회
    Page<Manager> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 특정 사용자 ID로 매니저 검색 (이름, 전화번호, 이메일)
    @Query("SELECT m FROM Manager m WHERE m.user.id = :userId AND " +
           "(m.name LIKE %:keyword% OR m.tel LIKE %:keyword% OR m.email LIKE %:keyword%) " +
            "ORDER BY m.createdAt DESC")
    Page<Manager> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
    
    // 특정 사용자 ID로 매니저 개수 조회
    long countByUserId(Long userId);
    
    // 특정 담당자를 제외하고 이메일 중복 확인
    boolean existsByEmailAndIdNot(String email, Long id);
}
