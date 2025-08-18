package didim.inquiry.controller.admin;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.User;
import didim.inquiry.dto.CustomerDto;
import didim.inquiry.service.AdminService;
import didim.inquiry.service.CustomerService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 고객코드 관리 컨트롤러
 * 고객코드 생성, 수정, 확인 기능을 처리합니다.
 */
@Controller
public class CustomerCodeController {

    private final AdminService adminService;
    private final CustomerService customerService;
    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;

    public CustomerCodeController(AdminService adminService, CustomerService customerService, UserService userService, AuthenticationHelper authenticationHelper) {
        this.adminService = adminService;
        this.customerService = customerService;
        this.userService = userService;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 고객코드 생성
     */
    @PostMapping("createCode")
    public String createCustomerCode(@ModelAttribute CustomerDto customerDto, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        //1. Dto에 요청 파라미터 값 담기
        //2. DB에 저장, 존재하는 코드일시 예외발생시켜 Try-Catch로 예외처리
        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);
            if (!"ADMIN".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("errorMessage", "권한이 없는 계정입니다.");
                return "redirect:/console";
            }

            adminService.createCustomerCode(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "고객코드가 성공적으로 등록되었습니다.");
            return "redirect:/console";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            System.err.println("고객코드 중복에러 발생 : " + e.getMessage());
            return "redirect:/console";
        }
    }

    /**
     * 고객코드 수정
     */
    @PostMapping("updateCode")
    public String updateCustomerCode(
            @ModelAttribute CustomerDto customerDto,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request) {

        //1. 수정 파라미터 받아서 업데이트
        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);
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

    /**
     * 고객코드 존재 여부 확인 API
     */
    @GetMapping("/api/check-customerCode")
    @ResponseBody
    public Map<String, Object> checkCustomerCode(@RequestParam String customerCode) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. 이미 해당 고객코드로 가입한 사용자가 있는지 확인
        if (userService.existsByCustomerCode(customerCode)) {
            response.put("exists", true);
            response.put("message", "이미 해당 고객코드로 가입한 사용자가 있습니다.");
            return response;
        }
        
        // 2. 고객코드가 존재하는지 확인
        Customer customer = customerService.getCustomerByCode(customerCode);
        if (customer == null) {
            response.put("exists", true);
            response.put("message", "존재하지 않는 고객코드입니다.");
            return response;
        }
        
        // 3. 고객코드가 활성 상태인지 확인
        if (!"ACTIVE".equals(customer.getStatus())) {
            response.put("exists", true);
            response.put("message", "비활성화된 고객코드입니다. 활성화 후 사용자 추가가 가능합니다.");
            return response;
        }
        
        // 모든 조건을 만족하면 사용 가능
        response.put("exists", false);
        response.put("message", "사용할 수 있는 고객코드입니다.");
        return response;
    }
}
