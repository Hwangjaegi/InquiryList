package didim.inquiry.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SearchInquiryDto {

    private String keyword; // 통합 검색어(제목, 담당자, 작성자)
    private String yearMonth ;
    private List<String> status;
    private String title;
    private String writer;
    private Long projectId; // 프로젝트 ID로 검색

}
