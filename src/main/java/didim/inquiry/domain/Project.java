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
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Project(){}

    //정적 팩토리 메서드 사용
    public static Project from(ProjectDto dto, Customer customer){
        Project project = new Project();
        project.setSubject(dto.getProjectSubject());
        project.setCustomer(customer);
        return project;
    }

    public void updateFrom(ProjectDto projectDto, Customer customer){
        this.subject = projectDto.getProjectSubject();
        this.customer = customer;
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
