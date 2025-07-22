package didim.inquiry.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true) // 코드 중복 방지
    private String code;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //데이터 입력시 자동 당일 날짜 , 상태 활성화값 입력 , 수정시는 변경X
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    //데이터 수정시 자동 당일 날짜입력 , 첫 입력시는 Null
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
