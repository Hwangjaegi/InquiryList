package didim.inquiry.dto;

import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ManagerDto {
    private long managerId;
    private String managerName;
    private String managerTel;
    private String managerEmail;
    private boolean managerDeleteFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User user;

    public ManagerDto(){

    }

    public ManagerDto(Manager manager){
        this.managerName = manager.getName();
        this.managerTel = manager.getTel();
        this.managerEmail = manager.getEmail();
        this.updatedAt = manager.getUpdatedAt();
    }

}
