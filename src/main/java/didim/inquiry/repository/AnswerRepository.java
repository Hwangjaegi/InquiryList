package didim.inquiry.repository;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer , Long> {
}
