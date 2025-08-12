package didim.inquiry.repository;

import didim.inquiry.domain.User;
import didim.inquiry.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Page<User> findAllByRoleOrderByIdDesc(String role , Pageable pageable);

    Page<User> findAllByRoleAndUsernameContainingOrEmailContainingOrderByIdDesc(String role, String keyword, String keyword2, Pageable pageable);
    // 다중 role 지원
    Page<User> findAllByRoleInOrderByIdDesc(List<String> roles, Pageable pageable);
    Page<User> findAllByRoleInAndUsernameContainingOrEmailContainingOrderByIdDesc(List<String> roles, String keyword, String keyword2, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.deleteFlag = true WHERE u.id = :id")
    void UserDeleteFlagTrueById(@Param("id") long id);

    @Modifying
    @Query("UPDATE User u SET u.deleteFlag = false WHERE u.id = :id")
    void UserDeleteFlagFalseById(@Param("id") Long id);



    //회원가입 시 고객코드로 가입한 유저가 있는지 확인
    List<User> findByCustomerCode(String customerCode);

    @Query("SELECT u FROM User u WHERE u.customerCode = :customerCode AND " +
            "(" +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "u.tel LIKE CONCAT('%', :search , '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.role) LIKE LOWER(CONCAT('%', :search , '%'))" +
            ")" +
            " ORDER BY u.createdAt DESC")
    Page<User> searchByCustomerCodeAndNameOrTelOrEmail(
            @Param("customerCode")String customerCode,
            @Param("search") String search,
            Pageable pageable);

    Page<User> findAllByCustomerCode(String customerCode, Pageable pageable);
    List<User> findAllByCustomerCode(String customerCode);

    Long countByCustomerCode(String customerCode);
    // ADMIN을 제외한 역할 카운트
    Long countByRoleIn(List<String> roles);

    @Query("SELECT u FROM User u WHERE u.role <> 'ADMIN' AND (" +
            "LOWER(u.customerCode) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "u.tel LIKE CONCAT('%', :search , '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.role) LIKE LOWER(CONCAT('%', :search , '%'))" +
            ") ORDER BY u.createdAt DESC")
    Page<User> searchAllFieldsExcludeAdmin(@Param("search") String search, Pageable pageable);


    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :id")
    void updateByRole(@Param("id") Long id, @Param("role") String role);

    // 고객코드 존재 여부 확인 (삭제되지 않은 사용자만)
    boolean existsByCustomerCodeAndDeleteFlagFalse(String customerCode);

    // 이메일 중복확인
    boolean existsByEmail(String email);

    Optional<User> findByUsernameAndCustomerCode(String username, String customerCode);
    
    // 고객코드와 역할로 사용자 조회 (USER 역할 중복 방지용)
    Optional<User> findByCustomerCodeAndRole(String customerCode, String role);
    
    // 현재 사용자를 제외한 모든 사용자 조회
    Page<User> findAllByIdNotOrderByIdDesc(Long userId, Pageable pageable);
    
    // 현재 사용자를 제외하고 검색
    @Query("SELECT u FROM User u WHERE u.id <> :userId AND (" +
            "LOWER(u.customerCode) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "u.tel LIKE CONCAT('%', :search , '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search , '%')) OR " +
            "LOWER(u.role) LIKE LOWER(CONCAT('%', :search , '%'))" +
            ") ORDER BY u.createdAt DESC")
    Page<User> searchAllFieldsExceptUser(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);
}
