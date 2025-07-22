package didim.inquiry.repository;

import didim.inquiry.domain.Manager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRepository extends JpaRepository<Manager , Long> {

    List<Manager> findByUserId(Long userId);

    List<Manager> findByUserIdAndDeleteFlagFalseOrderByCreatedAtDesc(Long userId);

    Page<Manager> findByUserIdAndDeleteFlagFalseOrderByCreatedAtDesc(Long userId , Pageable pageable);
    @Query("SELECT m FROM Manager m WHERE m.user.id = :userId AND m.deleteFlag = false AND " +
            "(" +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "m.tel LIKE CONCAT('%', :search, '%')" +
            ")" +
            " ORDER BY m.createdAt DESC")
    Page<Manager> searchByUserIdAndNameOrEmail(@Param("userId") Long userId,
                                               @Param("search") String search,
                                               Pageable pageable);

    @Modifying
    @Query("UPDATE Manager m SET m.deleteFlag = true WHERE m.id = :id")
    void updateManagerDeleteFlagById(@Param("id") Long id);
}
