package didim.inquiry.service;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.repository.AnswerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;

    public AnswerService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    public void saveAnswer(Answer answer){
        answerRepository.save(answer);
    }

    public Optional<Answer> getAnswerById(Long id) {
        return answerRepository.findById(id);
    }

    public Optional<Answer> getAnswerWithInquiryAndWriterById(Long id) {
        return answerRepository.findWithInquiryAndWriterById(id);
    }
}
