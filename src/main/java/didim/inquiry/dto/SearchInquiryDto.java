package didim.inquiry.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SearchInquiryDto {

    private String title;
    private String writer;
    private String yearMonth ;
    private List<String> status;

}
