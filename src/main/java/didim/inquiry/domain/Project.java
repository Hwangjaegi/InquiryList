package didim.inquiry.domain;

import didim.inquiry.dto.ProjectDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Project(){

    }

    //정적 팩토리 메서드 사용
    public static Project from(ProjectDto dto){
        Project project = new Project();
        project.setSubject(dto.getProjectSubject());
        project.setUser(dto.getUser());
        return project;
    }

    public void updateFrom(ProjectDto projectDto){
        this.subject = projectDto.getProjectSubject();
    }

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
