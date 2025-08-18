package didim.inquiry.controller;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.dto.CustomerDto;
import didim.inquiry.dto.ManagerDto;
import didim.inquiry.service.AdminService;
import didim.inquiry.service.ManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;

/**
 * 콘솔 화면 관련 컨트롤러
 * 어드민과 유저의 콘솔 화면을 처리합니다.
 */
@Controller
public class ConsoleController {

    private final AdminService adminService;
    private final ManagerService managerService;
    private final AuthenticationHelper authenticationHelper;

    public ConsoleController(AdminService adminService, ManagerService managerService, AuthenticationHelper authenticationHelper) {
        this.adminService = adminService;
        this.managerService = managerService;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 콘솔 화면 표시 (어드민, 유저 공통 URL 사용)
     */
    @GetMapping("/console")
    public String adminConsole(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);

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

            // 고객인 경우, 추후 manager권한 삭제
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
                return "page/userConsole";
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
}
