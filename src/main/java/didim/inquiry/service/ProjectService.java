package didim.inquiry.service;

import didim.inquiry.domain.Project;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.repository.ProjectRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import didim.inquiry.domain.Customer;

import java.time.LocalDateTime;


@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CustomerService customerService;

    public ProjectService(ProjectRepository projectRepository, CustomerService customerService) {
        this.projectRepository = projectRepository;
        this.customerService = customerService;
    }

    public Project getProject(long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트 입니다."));
    }

    public ProjectDto updateProject(ProjectDto projectDto, Customer customer) {
        Project project = projectRepository.findById(projectDto.getProjectId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트 입니다."));
        project.updateFrom(projectDto, customer);
        return ProjectDto.from(projectRepository.save(project));
    }

    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }

    public Page<Project> getAllProjects(Pageable pageable) {
        return projectRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    public Page<Project> getAllProjectsBySearch(String search, Pageable pageable) {
        return projectRepository.findAllBySubjectContainingOrderByCreatedAtDesc(search, pageable);
    }
    public Project saveProjectWithCustomer(ProjectDto projectDto, Customer customer) {
        Project project = Project.from(projectDto, customer);
        return projectRepository.save(project);
    }

    public Page<Project> getProjectListByCustomerId(Long customerId, Pageable pageable) {
        return projectRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }
    public Page<Project> getProjectListByCustomerIdAndSearch(Long customerId, String search, Pageable pageable) {
        return projectRepository.findByCustomerIdAndSubjectContainingOrderByCreatedAtDesc(customerId, search, pageable);
    }

    public Page<Project> getProjectListByCustomerCode(String customerCode, Pageable pageable) {
        // customerCode로 Customer를 조회 후, customerId로 프로젝트를 조회
        didim.inquiry.domain.Customer customer = customerService.findAll().stream()
            .filter(c -> customerCode.equals(c.getCode()))
            .findFirst()
            .orElse(null);
        if (customer == null) return Page.empty();
        return projectRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
    }

    public Page<Project> getAllProjectsByCustomerId(Long customerCodeId, Pageable pageable) {
        return projectRepository.findByCustomerIdOrderByCreatedAtDesc(customerCodeId , pageable);
    }

    public Page<Project> getAllProjectsByCustomerIdAndSearch(Long customerCodeId, String searchKeyword, Pageable pageable) {
        return projectRepository.findByCustomerIdAndSubjectContainingOrderByCreatedAtDesc(customerCodeId,searchKeyword,pageable);
    }

    // 서버구동 시 subject='기타문의' 프로젝트가 없으면 자동 생성
    @PostConstruct
    public void ensureEtcProjectExists() {
        if (projectRepository.findBySubject("기타문의").isEmpty()) {
            Project etc = new Project();
            etc.setSubject("기타문의");
            etc.setCreatedAt(LocalDateTime.now());
            projectRepository.save(etc);
        }
    }

    public Project getEtcProject() {
        return projectRepository.findBySubject("기타문의").orElseThrow(() -> new IllegalStateException("기타문의 프로젝트가 없습니다."));
    }
}
