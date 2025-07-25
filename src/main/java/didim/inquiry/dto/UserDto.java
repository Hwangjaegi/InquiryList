package didim.inquiry.dto;

import didim.inquiry.domain.User;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long id;
    private String username;    //아이디
    private String password;    //패스워드
    private String currentPassword; //현재 패스워드 검증용
    private String name;        //이름
    private String tel;         //전화번호
    private String email;       //이메일
    private String role;        //사용자역할
    private Boolean deleteFlag; //사용자삭제처리

    public UserDto(){

    }

    public UserDto(User user){
        this.id = user.getId();
        this.name = user.getName();
        this.tel = user.getTel();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.deleteFlag = user.getDeleteFlag();
    }
}
