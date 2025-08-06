package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import didim.inquiry.dto.CustomerDto;
import didim.inquiry.dto.ManagerDto;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.dto.UserDto;
import didim.inquiry.service.AdminService;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.ProjectService;
import didim.inquiry.service.UserService;
import didim.inquiry.service.CustomerService;
import didim.inquiry.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController extends BaseController {

    private final AdminService adminService;
    private final UserService userService;
    private final ProjectService projectService;
    private final CustomerService customerService;
    private final ManagerService managerService;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminController(AdminService adminService, UserService userService, ProjectService projectService, CustomerService customerService, ManagerService managerService, JwtTokenProvider jwtTokenProvider) {
        this.adminService = adminService;
        this.userService = userService;
        this.projectService = projectService;
        this.customerService = customerService;
        this.managerService = managerService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // JWT 토큰에서 사용자 정보 가져오기
    private User getCurrentUserFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String usernameWithCustomerCode = jwtTokenProvider.getUsernameFromToken(token);
            
            System.out.println("JWT 토큰에서 추출한 사용자명: " + usernameWithCustomerCode);
            
            // username|customerCode 형태인 경우
            if (usernameWithCustomerCode != null && usernameWithCustomerCode.contains("|")) {
                String[] parts = usernameWithCustomerCode.split("\\|");
                if (parts.length == 2) {
                    String username = parts[0];
                    String customerCode = parts[1];
                    return userService.getUserByUsernameAndCustomerCode(username, customerCode);
                }
            }
            
            // username만 있는 경우 (기존 방식)
            return userService.getUserByUsername(usernameWithCustomerCode);
        }
        return getCurrentUser(); // JWT 토큰이 없으면 세션 기반 인증 사용
    }

    // 요청에서 JWT 토큰 추출
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. URL 파라미터에서 토큰 확인
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam;
        }

        return null;
    }

    //콘솔 화면 표시
    @GetMapping("/console")
    public String adminConsole(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = getCurrentUserFromToken(request);

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
                return "page/adminConsole";
            }

            // 고객인 경우 , 추후 manager권한 삭제
            if ("USER".equals(user.getRole())) {
                Pageable pageable = PageRequest.of(page, size);

                Page<Manager> managerList;
                if (search != null && !search.trim().isEmpty()) {
                    managerList = managerService.searchManagersByUserId(user.getId(), search, pageable);
                } else {
                    managerList = managerService.getManagersByUserId(user.getId(), pageable);
                }

                // Manager를 ManagerDto로 변환
                Page<ManagerDto> managerDtoList = managerList.map(manager -> {
                    ManagerDto dto = new ManagerDto();
                    dto.setId(manager.getId());
                    dto.setName(manager.getName());
                    dto.setTel(manager.getTel());
                    dto.setEmail(manager.getEmail());
                    dto.setDeleteFlag(manager.isDeleteFlag());
                    dto.setCreatedAt(manager.getCreatedAt());
                    dto.setUpdatedAt(manager.getUpdatedAt());
                    dto.setUser(manager.getUser());
                    return dto;
                });

                Long totalManager = managerService.getManagerCountByUserId(user.getId());

                model.addAttribute("managerList", managerDtoList);
                model.addAttribute("currentUserId", user.getId());
                model.addAttribute("totalManager", totalManager);
                model.addAttribute("searchKeyword", search);
                return "page/managerConsole";
            }
        } catch (UsernameNotFoundException e) {
            System.err.println(e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
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
    public String createCustomerCode(@ModelAttribute CustomerDto customerDto, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        //1. Dto에 요청 파라미터 값 담기
        System.out.println("code : " + customerDto.getCode());
        System.out.println("설명 : " + customerDto.getCompany());

        //2. DB에 저장 , 존재하는 코드일시 예외발생시켜 Try-Catch로 예외처리
        try {
            User user = getCurrentUserFromToken(request);
            if (!"ADMIN".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/console";
            }


            System.out.println("메시지전달 --");
            adminService.createCustomerCode(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 등록되었습니다.");
            return "redirect:/console";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            System.out.println("고객코드 중복에러 발생 : " + e.getMessage());
            return "redirect:/console";
        }
    }

    //코드수정
    @PostMapping("updateCode")
    public String updateCustomerCode(
            @ModelAttribute CustomerDto customerDto,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request) {

        System.out.println("수정코드 : " + customerDto.getCode());
        System.out.println("수정설명 : " + customerDto.getCompany());
        System.out.println("수정상태 : " + customerDto.getStatus());

        //1. 수정 파라미터 받아서 업데이트
        try {
            User user = getCurrentUserFromToken(request);
            if (!"ADMIN".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/console";
            }
            
            adminService.updateCustomerCode(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/console?page=" + page + (search != null ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
    }

//    //코드삭제
//    @PostMapping("/admin/deleteCode/{id}")
//    public String deleteCustomerCode(
//            @PathVariable Long id,
//            RedirectAttributes redirectAttributes,
//            @RequestParam(value = "page", defaultValue = "0") int page,
//            @RequestParam(value = "size", defaultValue = "10") int size,
//            @RequestParam(value = "search", required = false) String search,
//            HttpServletRequest request) {
//
//        //해당 고객코드가 없을 경우 예외처리 후 삭제
//        try {
//            User validateUser = getCurrentUserFromToken(request);
//            if (!validateUser.getRole().equals("ADMIN")) {
//                throw new IllegalArgumentException("권한이 없는 계정입니다.");
//            }
//
//            adminService.deleteCustomerCode(id);
//            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 삭제되었습니다");
//        } catch (IllegalArgumentException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//        } catch (DataIntegrityViolationException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트가 등록된 고객코드는 삭제 할 수 없습니다");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
//        }
//
//        return "redirect:/console?page=" + page + "&size=" + size + (search != null ? "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8) : "");
//    }

    //프로젝트 리스트
    @GetMapping("/projectList")
    public String projectList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = getCurrentUserFromToken(request);
            Pageable pageable = PageRequest.of(page, size);
            String customerCode = user.getCustomerCode();
            Customer customer = customerService.getCustomerByCode(customerCode);

            // 검색 조회 , 일반 조회
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

    @GetMapping("/admin/projectListAdmin")
    public String adminProjectList(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "size", defaultValue = "10") int size,
                                   @RequestParam(value = "search", required = false) String searchKeyword,
                                   Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        try {
            User validateUser = getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

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
            // 프로젝트 통계 추가
            model.addAttribute("totalProjects", projectService.countAllProjects());
            model.addAttribute("newProjects", projectService.countNewProjectsThisMonth());
            return "page/adminProjectList";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/console";
        }
    }

    @PostMapping("/admin/projectAdd")
    public String addProject(@RequestParam Long customerId,
                             @RequestParam String projectSubject,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        try {
            User validateUser = getCurrentUserFromToken(request);
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
            User validateUser = getCurrentUserFromToken(request);
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

    @PostMapping("/admin/deleteProject/{id}")
    public String deleteProject(@PathVariable Long id,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        try {
            User validateUser = getCurrentUserFromToken(request);
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

    @GetMapping("/admin/customerList")
    public String customerList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            //토큰이 있을때는 토큰검증 , 없을때는 세션 검증
            User validateUser = getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }

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

            // 각 사용자의 고객코드 상태 확인
            List<User> userList = userPage.getContent();
            Map<String, Boolean> customerStatusMap = new HashMap<>();
            for (User user : userList) {
                if (!customerStatusMap.containsKey(user.getCustomerCode())) {
                    customerStatusMap.put(user.getCustomerCode(), userService.isCustomerCodeActive(user.getCustomerCode()));
                }
            }

            model.addAttribute("userList", userList);
            model.addAttribute("customerStatusMap", customerStatusMap);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("newUsers", newUsers);
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("searchKeyword", searchKeyword);

            return "page/adminCustomerList";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Not Found Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "알수없는 오류가 발생 했습니다 : " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/admin/updateUser")
    public String updateUser(
            UserDto requestUserDto,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User currentUser = getCurrentUserFromToken(request);
            if (!currentUser.getRole().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/login";
            }

            // 사용자 정보 업데이트
            UserDto userDto = new UserDto();
            userDto.setId(requestUserDto.getId());
            userDto.setUsername(requestUserDto.getUsername());
            userDto.setName(requestUserDto.getName());
            userDto.setEmail(requestUserDto.getEmail());
            userDto.setTel(requestUserDto.getTel());
            userDto.setRole(requestUserDto.getRole());

            userService.updateUserByAdmin(userDto);
            redirectAttributes.addFlashAttribute("successMessage", "사용자 정보가 성공적으로 수정되었습니다.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            System.err.println("사용자 수정 중 오류 발생: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "사용자 정보 수정 중 오류가 발생했습니다.");
        }

        return "redirect:/admin/customerList";
    }

    @PostMapping("/admin/deleteUser/{id}")
    public String deleteUser(
            @PathVariable long id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = getCurrentUserFromToken(request);

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
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = getCurrentUserFromToken(request);

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

    @GetMapping("/myInfo")
    public String myInfo(Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        System.out.println("요청전달");
        try {
            User user = getCurrentUserFromToken(request);
            model.addAttribute("user", user);
            model.addAttribute("role", user.getRole());
            return "page/myInfo";
        } catch (UsernameNotFoundException e) {
            System.err.println("User Not Found Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // 고객코드 존재 여부 확인 API
    @GetMapping("/api/check-customerCode")
    @ResponseBody
    public Map<String, Object> checkCustomerCode(@RequestParam String customerCode) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("user : " + userService.existsByCustomerCode(customerCode) + " / customer : " + customerService.existsByCustomerCodeAndStatusActive(customerCode));
        if (userService.existsByCustomerCode(customerCode)) {
            response.put("exists", true);
        } else if (!customerService.existsByCustomerCodeAndStatusActive(customerCode)) {
            response.put("exists", true);
        } else {
            response.put("exists", false);
        }

        return response;
    }
}
