package didim.inquiry.repository;

import didim.inquiry.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);

    Page<User> findAllByRoleOrderByIdDesc(String role , Pageable pageable);

    Page<User> findAllByRoleAndUsernameContainingOrEmailContainingOrderByIdDesc(String role, String keyword, String keyword2, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.deleteFlag = true WHERE u.id = :id")
    void UserDeleteFlagTrueById(@Param("id") long id);

    @Modifying
    @Query("UPDATE User u SET u.deleteFlag = false WHERE u.id = :id")
    void UserDeleteFlagFalseById(@Param("id") Long id);

    //회원가입시 고객코드가 같은 유저중 아이디가 같은지 확인
    Optional<User> findByUsernameAndCustomerCode(String username, String customerCode);
}
