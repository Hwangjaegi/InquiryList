package didim.inquiry.domain;

import didim.inquiry.dto.UserDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username"})
        }
)
@Getter @Setter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerCode;//고객코드
    private String username;    //아이디
    private String password;    //패스워드
    private String name;        //이름
    private String tel;         //전화번호

    private String email;       //이메일
    private String role;        //사용자역할
    private Boolean deleteFlag = false; //사용자삭제처리
    private LocalDateTime createdAt; //생성날짜
    private LocalDateTime updatedAt; //수정날짜


    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
