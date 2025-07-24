package didim.inquiry.repository;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer , Long> {

    @Query("SELECT a FROM Answer a JOIN FETCH a.inquiry i JOIN FETCH i.writer WHERE a.id = :id")
    Optional<Answer> findWithInquiryAndWriterById(@Param("id") Long id);
}
