package didim.inquiry.service;

import didim.inquiry.domain.Project;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Page<Project> getProjectBySearchList(Long user_id , String search , Pageable pageable) {
        return projectRepository.findByUserIdAndSubjectContainingOrderByCreatedAtDesc(user_id, search, pageable);
    }
    public Page<Project> getProjectList(Long user_id , Pageable pageable) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user_id , pageable);
    }
    public List<Project> getProjectList(Long user_id ) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user_id);
    }

    public Project getProject(long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트 입니다."));
    }

    public Project saveProject(ProjectDto projectDto) {
        Project project = Project.from(projectDto); //static 정적팩토리 메서드사용방법
        return projectRepository.save(project);
    }

    public ProjectDto updateProject(ProjectDto projectDto) {
        Project project = projectRepository.findById(projectDto.getProjectId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트 입니다."));
        project.updateFrom(projectDto);

        return ProjectDto.from(projectRepository.save(project));
    }

    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }
}
