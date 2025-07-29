package didim.inquiry.dto;

import didim.inquiry.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ManagerDto {
    private long id;
    private String name;
    private String tel;
    private String email;
    private boolean deleteFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User user;
}
