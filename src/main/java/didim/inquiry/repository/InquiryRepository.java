package didim.inquiry.repository;

import didim.inquiry.domain.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByWriter_Username(String username);

    // 문의 및 답변 전체 조회
    @Query("SELECT DISTINCT i FROM Inquiry i LEFT JOIN FETCH i.answers ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllWithAnswers(Pageable pageable);

    @Query("SELECT i FROM Inquiry i LEFT JOIN FETCH i.answers WHERE i.writer.username = :username ORDER BY i.createdAt DESC")
    List<Inquiry> findWithAnswersByUsername(String username);

    // Inquiry 조회 시 User 엔티티의 username을 가져올 때 맵핑
    @EntityGraph(attributePaths = {"answers", "answers.user"})
    @Query("SELECT i FROM Inquiry i JOIN i.writer w WHERE w.username = :username ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesByAnswersWithUserByUsername(String username, Pageable pageable);

    // 관리자용: 검색 조건에 따른 문의 조회
    @EntityGraph(attributePaths = {"answers", "answers.user"})
    @Query("SELECT i FROM Inquiry i JOIN i.writer w " +
            "WHERE (:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:writer IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :writer, '%'))) " +
            "AND (:start IS NULL OR i.createdAt >= :start) " +
            "AND (:end IS NULL OR i.createdAt <= :end) " +
            "AND (:statuses IS NULL OR i.status IN :statuses) " +
            "ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesBySearchWithAdmin(
            @Param("title") String title,
            @Param("writer") String writer,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<String> statuses,
            Pageable pageable);

    // 사용자용: 본인 문의만 검색
    @EntityGraph(attributePaths = {"answers", "answers.user"})
    @Query("SELECT i FROM Inquiry i JOIN i.writer w " +
            "WHERE (:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:writer IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :writer, '%'))) " +
            "AND (:start IS NULL OR i.createdAt >= :start) " +
            "AND (:end IS NULL OR i.createdAt <= :end) " +
            "AND (:statuses IS NULL OR i.status IN :statuses) " +
            "AND w.username = :username " +
            "ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesBySearchWithUserByUsername(
            @Param("title") String title,
            @Param("writer") String writer,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<String> statuses,
            @Param("username") String username,
            Pageable pageable);

    //년도의 뒷자리2개를 가져와 해당 년도를 포함하는 날짜값이 있을경우 맨 마지막 값을 가져옴
    @Query(value = "SELECT tick_number FROM inquiry WHERE tick_number LIKE CONCAT(:year, '%') ORDER BY tick_number DESC LIMIT 1", nativeQuery = true)
    String findLastTickNumber(@Param("year") String year);

    @EntityGraph(attributePaths = {"answers", "answers.user"})
    @Query("SELECT i FROM Inquiry i JOIN i.writer w " +
            "WHERE ( :keyword IS NULL OR " +
            "LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "i.tickNumber LIKE CONCAT('%', :keyword, '%') OR " +
            "LOWER(w.customerCode) LIKE LOWER(CONCAT('%', :keyword , '%')) " +
            ") " +
            "AND (:statuses IS NULL OR i.status IN :statuses) " +
            "AND (:start IS NULL OR i.createdAt >= :start) " +
            "AND (:end IS NULL OR i.createdAt <= :end) " +
            "ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesByKeywordForAdmin(@Param("keyword") String keyword, @Param("statuses") List<String> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"answers", "answers.user"})
    @Query("SELECT i FROM Inquiry i JOIN i.writer w " +
            "WHERE ( :keyword IS NULL OR " +
            "LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "i.tickNumber LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:statuses IS NULL OR i.status IN :statuses) " +
            "AND (:start IS NULL OR i.createdAt >= :start) " +
            "AND (:end IS NULL OR i.createdAt <= :end) " +
            "AND w.username = :username " +
            "ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesByKeywordForUser(@Param("keyword") String keyword, @Param("statuses") List<String> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("username") String username, Pageable pageable);

}