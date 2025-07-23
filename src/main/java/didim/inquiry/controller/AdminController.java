package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import didim.inquiry.dto.CustomerDto;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.service.AdminService;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.ProjectService;
import didim.inquiry.service.UserService;
import didim.inquiry.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import didim.inquiry.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
public class AdminController extends BaseController {

    private final AdminService adminService;
    private final UserService userService;
    private final ManagerService managerService;
    private final ProjectService projectService;
    private final CustomerService customerService;

    public AdminController(AdminService adminService, UserService userService, ManagerService managerService, ProjectService projectService, CustomerService customerService) {
        this.adminService = adminService;
        this.userService = userService;
        this.managerService = managerService;
        this.projectService = projectService;
        this.customerService = customerService;
    }

    //관리자 화면 표시
    @GetMapping("admin/console")
    public String adminConsole(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentUser();

            // 어드민인 경우
            if ("ADMIN".equals(user.getRole())) {
                Pageable pageable = PageRequest.of(page, size);

                Page<Customer> customerList;
                if (search != null && !search.trim().isEmpty()) {
                    customerList = adminService.getCustomerCodeList(search, pageable);
                } else {
                    customerList = adminService.getCustomerCodeList(null, pageable);
                }

                Page<CustomerDto> customerDtoList = customerList.map(CustomerDto::new);

                long totalCustomer = adminService.getCustomerCount();
                long activeCustomer = adminService.getActiveCustomerCount();

                YearMonth currentMonth = YearMonth.now();
                long newCustomer = adminService.getNewCustomerCountByMonth(currentMonth);

                model.addAttribute("customers", customerDtoList);
                model.addAttribute("totalCustomer", totalCustomer);
                model.addAttribute("activeCustomer", activeCustomer);
                model.addAttribute("newCustomer", newCustomer);
                model.addAttribute("searchKeyword", search);
                return "admin/adminConsole";
            }

            // 관리자인 경우
            if ("MANAGER".equals(user.getRole())) {
                Pageable pageable = PageRequest.of(page, size);

                Page<User> userListByCustomerCode;
                if (search != null && !search.trim().isEmpty()) {
                    userListByCustomerCode = userService.searchUsersByCustomerCode(user.getCustomerCode(), search, pageable);
                } else {
                    userListByCustomerCode = userService.getUsersByCustomerCode(user.getCustomerCode(),pageable);
                }

                Long totalUser = userService.getUsersCountByCustomerCode(user.getCustomerCode());

                model.addAttribute("userList", userListByCustomerCode);
                model.addAttribute("currentUserId",user.getId());
                model.addAttribute("totalUser", totalUser);
                model.addAttribute("searchKeyword", search);
                return "admin/managerConsole";
            }

            // 일반 사용자
            if ("USER".equals(user.getRole())) {
                Pageable pageable = PageRequest.of(page, size);

                Page<Manager> managerList;
                if (search != null && !search.trim().isEmpty()) {
                    managerList = managerService.getManagerList(user.getId(), search, pageable);
                } else {
                    managerList = managerService.getManagerList(user.getId(), null, pageable);
                }

                Long totalManager = managerService.getManagerCount();

                model.addAttribute("managerList", managerList);
                model.addAttribute("totalManager", totalManager);
                model.addAttribute("searchKeyword", search);
                return "admin/userConsole";
            }
        } catch (UsernameNotFoundException e) {
            System.err.println(e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e){
            // 알 수 없는 역할
            System.err.println("Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "권한이 없습니다.");
        return "redirect:/login";
    }

    //코드생성
    @PostMapping("createCode")
    public String createCustomerCode(@ModelAttribute CustomerDto customerDto, RedirectAttributes redirectAttributes, Model model) {
        //1. Dto에 요청 파라미터 값 담기
        System.out.println("code : " + customerDto.getCode());
        System.out.println("설명 : " + customerDto.getDescription());

        //2. DB에 저장 , 존재하는 코드일시 예외발생시켜 Try-Catch로 예외처리
        try {
            adminService.createCustomerCode(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 등록되었습니다.");
            return "redirect:/admin/console";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            System.out.println("고객코드 중복에러 발생 : " + e.getMessage());
            return "redirect:/admin/console";
        }
    }

    //코드수정
    @PostMapping("updateCode")
    public String updateCustomerCode(
            @ModelAttribute CustomerDto customerDto,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "search", required = false) String search) {

        System.out.println("수정코드 : " + customerDto.getCode());
        System.out.println("수정설명 : " + customerDto.getDescription());
        System.out.println("수정상태 : " + customerDto.getStatus());

        //1. 수정 파라미터 받아서 업데이트
        try {
            adminService.updateCustomerCode(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/console?page=" + page + (search != null ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
    }

    //코드삭제
    @PostMapping("deleteCode/{id}")
    public String deleteCustomerCode(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search) {

        System.out.println("삭제 코드 값 : " + id);

        //해당 고객코드가 없을 경우 예외처리 후 삭제
        try {
            adminService.deleteCustomerCode(id);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 삭제되었습니다");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/console?page=" + page + "&size=" + size + (search != null ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
    }

    //프로젝트 리스트
    @GetMapping("/admin/projectList")
    public String projectList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = getCurrentUser();
            Pageable pageable = PageRequest.of(page, size);

            Page<Project> projectList = searchKeyword == null || searchKeyword.isEmpty()
                    ? projectService.getAllProjects(pageable)
                    : projectService.getAllProjectsBySearch(searchKeyword, pageable);


            model.addAttribute("projectList", projectList);
            model.addAttribute("searchKeyword", searchKeyword);
            model.addAttribute("currentPage", projectList.getNumber());
            model.addAttribute("totalPages", projectList.getTotalPages());
            model.addAttribute("pageSize", projectList.getSize());

            return "admin/userProjectList";
        } catch (UsernameNotFoundException e) {
            System.err.println("Not Found Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/admin/projectListAdmin")
    public String adminProjectList(@RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                  @RequestParam(value = "search", required = false) String searchKeyword,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Project> projectList = (searchKeyword == null || searchKeyword.isEmpty())
                    ? projectService.getAllProjects(pageable)
                    : projectService.getAllProjectsBySearch(searchKeyword, pageable);
            model.addAttribute("projectList", projectList.getContent());
            model.addAttribute("currentPage", projectList.getNumber());
            model.addAttribute("totalPages", projectList.getTotalPages());
            model.addAttribute("pageSize", projectList.getSize());
            model.addAttribute("searchKeyword", searchKeyword);
            // 전체 customerCode 리스트
            model.addAttribute("customerList", customerService.findAll());
            return "admin/adminProjectList";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/console";
        }
    }

    @PostMapping("/admin/projectAdd")
    public String addProject(@RequestParam Long customerId,
                            @RequestParam String projectSubject,
                            RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerService.findById(customerId);
            ProjectDto dto = new ProjectDto();
            dto.setProjectSubject(projectSubject);
            projectService.saveProjectWithCustomer(dto, customer);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 추가되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/projectListAdmin";
    }

    @PostMapping("/admin/updateProject")
    public String updateProject(@RequestParam Long projectId,
                                @RequestParam String projectSubject,
                                @RequestParam Long customerId,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                RedirectAttributes redirectAttributes) {
        try {
            ProjectDto dto = new ProjectDto();
            dto.setProjectId(projectId);
            dto.setProjectSubject(projectSubject);
            Customer customer = customerService.findById(customerId);
            projectService.updateProject(dto, customer);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        String url = "/admin/projectListAdmin?page=" + page + "&size=" + size + (search != null && !search.isEmpty() ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
        return "redirect:" + url;
    }

    @PostMapping("/admin/deleteProject/{id}")
    public String deleteProject(@PathVariable Long id,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                RedirectAttributes redirectAttributes) {
        try {
            projectService.deleteProjectById(id);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        String url = "/admin/projectListAdmin?page=" + page + "&size=" + size + (search != null && !search.isEmpty() ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
        return "redirect:" + url;
    }

    @GetMapping("/admin/customerList")
    public String customerList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage;
            if (searchKeyword == null || searchKeyword.isEmpty()) {
                // ADMIN을 제외한 모든 사용자
                List<String> roles = Arrays.asList("USER", "MANAGER");
                userPage = userService.getUsersByRole(roles, pageable);
            } else {
                // ADMIN을 제외하고 모든 필드로 검색
                userPage = userService.searchAllFieldsExcludeAdmin(searchKeyword, pageable);
            }

            long totalUsers = userService.getUsersCountByRoles(Arrays.asList("USER", "MANAGER"));
            long newUsers = userPage.getContent().stream()
                    .filter(user -> user.getCreatedAt() != null &&
                            user.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1)))
                    .count();

            model.addAttribute("userList", userPage.getContent());
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("newUsers", newUsers);
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("searchKeyword", searchKeyword);

            return "admin/adminCustomerList";
        } catch (Exception e) {
            System.err.println("Not Found Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/admin/deleteUser/{id}")
    public String deleteUser(
            @PathVariable long id,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentUser();

            if (!user.getRole().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/login";
            }

            userService.deleteUserById(id);

            redirectAttributes.addFlashAttribute("successMessage", "정상적으로 삭제 되었습니다.");
            return "redirect:/admin/customerList";

        } catch (UsernameNotFoundException e) {
            System.err.println("User Not Found Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "알 수없는 오류가 발생했습니다.");
            return "redirect:/admin/customerList";
        }
    }

    @PostMapping("/admin/restoreUser/{id}")
    public String restoreUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentUser();

            if (!user.getRole().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/login";
            }

            userService.restoreUserById(id);
            redirectAttributes.addFlashAttribute("successMessage", "정상적으로 복원 되었습니다.");
            return "redirect:/admin/customerList";

        } catch (UsernameNotFoundException e) {
            System.err.println("User Not Found Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "알 수없는 오류가 발생했습니다.");
            return "redirect:/admin/customerList";
        }
    }

    @GetMapping("/admin/myInfo")
    public String myInfo(Model model) {
        User user = getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole());
        return "admin/myInfo";
    }
}
