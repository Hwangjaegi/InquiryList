package didim.inquiry.dto;

import didim.inquiry.domain.Customer;
import didim.inquiry.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerDto {
    private Long id;
    private String code;
    private String company;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private User user;

    //다른 생성자 생성 시 기본 생성자 필수로 추가
    public CustomerDto(){

    }

    //도메인 -> Dto 변환 생성자
    public CustomerDto(Customer customer){
        this.id = customer.getId();
        this.code = customer.getCode();
        this.company = customer.getCompany();
        this.status = customer.getStatus();
        this.createAt = customer.getCreatedAt();
        this.updateAt = customer.getUpdatedAt();
    }
}
