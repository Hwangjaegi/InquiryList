package didim.inquiry.dto;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InquiryDto {
    private Long id;                //문의고유번호
    private String title;           //문의제목
    private String content;         //문의내용
    private LocalDateTime createdAt; //생성날짜
    private String status;          //문의상태
    private User writer;          //작성자 이름 또는 ID
    private User assignee;        //담당자 이름 또는 ID
    private List<Answer> answers = new ArrayList<>() ; //답변 리스트
    private long answerCount;

}
