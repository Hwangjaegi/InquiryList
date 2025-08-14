package didim.inquiry.controller.admin;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.User;
import didim.inquiry.dto.UserDto;
import didim.inquiry.service.CustomerService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 관리 컨트롤러
 * 사용자 조회, 수정, 삭제, 복원 기능을 처리합니다.
 */
@Controller
public class UserManagementController{

    private final UserService userService;
    private final CustomerService customerService;
    private final AuthenticationHelper authenticationHelper;

    public UserManagementController(UserService userService, CustomerService customerService, AuthenticationHelper authenticationHelper) {
        this.userService = userService;
        this.customerService = customerService;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 어드민용 사용자 리스트 조회
     */
    @GetMapping("/admin/customerList")
    public String customerList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String searchKeyword,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            //토큰이 있을때는 토큰검증, 없을때는 세션 검증
            User validateUser = authenticationHelper.getCurrentUserFromToken(request);
            if (!validateUser.getRole().equals("ADMIN")) {
                throw new IllegalArgumentException("권한이 없는 계정입니다.");
            }            

            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage;
            
            if (searchKeyword == null || searchKeyword.isEmpty()) {
                if (includeInactive) {
                    // 비활성화된 사용자 포함하여 조회
                    userPage = userService.getAllUsersExceptCurrentIncludeInactive(validateUser.getId(), pageable);
                } else {
                    // 고객코드가 활성화된 사용자만 조회
                    userPage = userService.getAllUsersExceptCurrent(validateUser.getId(), pageable);
                }
            } else {
                if (includeInactive) {
                    // 비활성화된 사용자 포함하여 검색
                    userPage = userService.searchAllUsersExceptCurrentIncludeInactive(validateUser.getId(), searchKeyword, pageable);
                } else {
                    // 고객코드가 활성화된 사용자만 검색
                    userPage = userService.searchAllUsersExceptCurrent(validateUser.getId(), searchKeyword, pageable);
                }
            }

            // 전체 사용자 수 (현재 사용자 제외)
            long totalUsers = userService.getUsersCount() - 1;
            long newUsers = userPage.getContent().stream()
                    .filter(user -> user.getCreatedAt() != null &&
                            user.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1)))
                    .count();

            // 각 사용자의 고객코드 상태 확인 (최적화: 한 번의 쿼리로 모든 고객코드 상태 조회)
            List<User> userList = userPage.getContent();
            Map<String, Boolean> customerStatusMap = new HashMap<>();
            
            // 고유한 고객코드들만 추출
            Set<String> uniqueCustomerCodes = userList.stream()
                    .map(User::getCustomerCode)
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

            // 현재 로그인한 사용자 정보를 모델에 추가
            model.addAttribute("currentUser", validateUser);
            model.addAttribute("userList", userList);
            model.addAttribute("customerStatusMap", customerStatusMap);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("newUsers", newUsers);
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("searchKeyword", searchKeyword);
            model.addAttribute("includeInactive", includeInactive);

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

    /**
     * 사용자 정보 수정
     */
    @PostMapping("/admin/updateUser")
    public String updateUser(
            UserDto requestUserDto,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User currentUser = authenticationHelper.getCurrentUserFromToken(request);
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

    /**
     * 사용자 삭제
     */
    @PostMapping("/admin/deleteUser/{id}")
    public String deleteUser(
            @PathVariable long id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);

            if (!user.getRole().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/login";
            }

            userService.softDeleteUser(id);

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

    /**
     * 사용자 복원
     */
    @PostMapping("/admin/restoreUser/{id}")
    public String restoreUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);

            if (!user.getRole().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/login";
            }

            userService.restoreUser(id);
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
}
