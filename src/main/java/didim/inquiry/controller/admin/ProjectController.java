package didim.inquiry.controller.admin;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.service.CustomerService;
import didim.inquiry.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 프로젝트 관리 컨트롤러
 * 프로젝트 조회, 생성, 수정, 삭제 기능을 처리합니다.
 */
@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final CustomerService customerService;
    private final AuthenticationHelper authenticationHelper;

    public ProjectController(ProjectService projectService, CustomerService customerService, AuthenticationHelper authenticationHelper) {
        this.projectService = projectService;
        this.customerService = customerService;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 유저용 프로젝트 리스트 조회
     */
    @GetMapping("/projectList")
    public String projectList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);
            Pageable pageable = PageRequest.of(page, size);
            String customerCode = user.getCustomerCode();
            Customer customer = customerService.getCustomerByCode(customerCode);

            // 검색 조회, 일반 조회
            Page<Project> projectList = searchKeyword == null || searchKeyword.isEmpty()
                    ? projectService.getAllProjectsByCustomerId(customer.getId(), pageable)
                    : projectService.getAllProjectsByCustomerIdAndSearch(customer.getId(), searchKeyword, pageable);

            model.addAttribute("projectList", projectList);
            model.addAttribute("role", user.getRole());
            model.addAttribute("searchKeyword", searchKeyword);
            model.addAttribute("currentPage", projectList.getNumber());
            model.addAttribute("totalPages", projectList.getTotalPages());
            model.addAttribute("pageSize", projectList.getSize());

            return "page/userProjectList";
        } catch (UsernameNotFoundException e) {
            System.err.println("Not Found Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    /**
     * 어드민용 프로젝트 리스트 조회
     */
    @GetMapping("/admin/projectListAdmin")
    public String adminProjectList(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "size", defaultValue = "10") int size,
                                   @RequestParam(value = "search", required = false) String searchKeyword,
                                   @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
                                   Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        try {
            User currentUser = authenticationHelper.getCurrentUserFromToken(request);
            if (!currentUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Project> pageProject;
            
            if (searchKeyword == null || searchKeyword.isEmpty()) {
                if (includeInactive) {
                    // 비활성화된 프로젝트 포함하여 조회
                    pageProject = projectService.getAllProjectsIncludeInactive(pageable);
                } else {
                    // 고객코드가 활성화된 프로젝트만 조회
                    pageProject = projectService.getAllProjects(pageable);
                }
            } else {
                if (includeInactive) {
                    // 비활성화된 프로젝트 포함하여 검색
                    pageProject = projectService.getAllProjectsBySearchIncludeInactive(searchKeyword, pageable);
                } else {
                    // 고객코드가 활성화된 프로젝트만 검색
                    pageProject = projectService.getAllProjectsBySearch(searchKeyword, pageable);
                }
            }

            //프로젝트
            List<Customer> activeCustomer = customerService.findAllByActive("ACTIVE");
            long countAllProjects = projectService.countAllProjects();
            long countNewProjects = projectService.countNewProjectsThisMonth();

            //각 프로젝트 고객코드 상태 확인 (최적화: 한 번의 쿼리로 모든 고객코드 상태 조회)
            List<Project> projectList = pageProject.getContent();
            Map<String, Boolean> customerStatusMap = new HashMap<>();
            
            // 고유한 고객코드들만 추출
            Set<String> uniqueCustomerCodes = projectList.stream()
                    .map(project -> project.getCustomer().getCode())
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toSet());
            
            // 한 번의 쿼리로 모든 고객코드의 상태 조회
            if (!uniqueCustomerCodes.isEmpty()) {
                List<Customer> activeCustomers = customerService.findAllByCodeInAndStatus(uniqueCustomerCodes, "ACTIVE");
                Set<String> activeCustomerCodes = activeCustomers.stream()
                        .map(Customer::getCode)
                        .collect(Collectors.toSet());
                
                for (String customerCode : uniqueCustomerCodes) {
                    customerStatusMap.put(customerCode, activeCustomerCodes.contains(customerCode));
                }
            }

            model.addAttribute("projectList", projectList);
            model.addAttribute("customerStatusMap", customerStatusMap);
            model.addAttribute("currentPage", pageProject.getNumber());
            model.addAttribute("totalPages", pageProject.getTotalPages());
            model.addAttribute("pageSize", pageProject.getSize());
            model.addAttribute("searchKeyword", searchKeyword);
            model.addAttribute("includeInactive", includeInactive);
            // 전체 active customerCode 리스트
            model.addAttribute("customerList", activeCustomer);
            // 프로젝트 통계 추가
            model.addAttribute("totalProjects", countAllProjects);
            model.addAttribute("newProjects", countNewProjects);
            return "page/adminProjectList";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("오류 발생 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/console";
        }
    }

    /**
     * 프로젝트 추가
     */
    @PostMapping("/admin/projectAdd")
    public String addProject(@RequestParam Long customerId,
                             @RequestParam String projectSubject,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        try {
            User validateUser = authenticationHelper.getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

            Customer customer = customerService.getCustomerById(customerId);
            ProjectDto dto = new ProjectDto();
            dto.setProjectSubject(projectSubject);
            projectService.saveProjectWithCustomer(dto, customer);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/projectListAdmin";
    }

    /**
     * 프로젝트 수정
     */
    @PostMapping("/admin/updateProject")
    public String updateProject(@RequestParam Long projectId,
                                @RequestParam String projectSubject,
                                @RequestParam Long customerId,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        try {
            User validateUser = authenticationHelper.getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

            ProjectDto dto = new ProjectDto();
            dto.setProjectId(projectId);
            dto.setProjectSubject(projectSubject);
            Customer customer = customerService.getCustomerById(customerId);
            projectService.updateProject(dto, customer);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        String url = "/admin/projectListAdmin?page=" + page + "&size=" + size + (search != null && !search.isEmpty() ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
        return "redirect:" + url;
    }

    /**
     * 프로젝트 삭제
     */
    @PostMapping("/admin/deleteProject/{id}")
    public String deleteProject(@PathVariable Long id,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        try {
            User validateUser = authenticationHelper.getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

            projectService.deleteProjectById(id);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("프로젝트 삭제중 문제발생 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "문의가 등록된 프로젝트는 삭제 할 수 없습니다.");
        }
        String url = "/admin/projectListAdmin?page=" + page + "&size=" + size + (search != null && !search.isEmpty() ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
        return "redirect:" + url;
    }
}
