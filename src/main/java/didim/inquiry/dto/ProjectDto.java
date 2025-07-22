package didim.inquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectDto {
    private long projectId;
    private String projectSubject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonIgnore
    private User user;

    public ProjectDto(){

    }

    public static ProjectDto from(Project project){
        ProjectDto projectDto = new ProjectDto();
        projectDto.setProjectId(project.getId());
        projectDto.setProjectSubject(project.getSubject());
        projectDto.setCreatedAt(project.getCreatedAt());
        projectDto.setUpdatedAt(project.getUpdatedAt());

        return projectDto;
    }
}
