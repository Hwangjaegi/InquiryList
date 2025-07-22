package didim.inquiry.dto;

import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnswerDto {
    private Long id;
    private String content;
    private LocalDateTime repliedAt;
    private Inquiry inquiry;
    private User user;

}
