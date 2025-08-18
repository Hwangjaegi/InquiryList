package didim.inquiry.controller;

import didim.inquiry.common.BaseController;
import didim.inquiry.domain.User;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import didim.inquiry.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class UserController extends BaseController{

    private final UserService userService;
    private final ManagerService managerService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, ManagerService managerService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.managerService = managerService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home(){
        return "login";
    }


    //로그인 요청 처리 (시큐리티 Config 처리)
    @GetMapping("/login")
    public String login(@RequestParam(value = "error" , required = false) String error,
                        Model model,
                        HttpServletResponse response){

        //뒤로가기 시 캐싱데이터 사용을 사용안함 처리
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies

        if (error != null){
            System.err.println(error);
            model.addAttribute("loginErrorMessage" , "일치하는 계정 정보가 존재하지 않습니다");
        }
        return "login";
    }

    //회원가입 요청 처리
    @PostMapping("/signup")
    public String joinUser(User user , Model model){
        //순서 : 유저코드가 존재하는지 , 유저코드로 처음 가입하는 유저일경우 권한부여 이후로는 일반 사용자권한

        //고객코드가 존재하는지 확인 + 고객코드가 같은 유저중 아이디가 같은 경우가 존재하는지 확인 후 가입처리
        boolean success = userService.signUpUser(user);

        if (!success){
            model.addAttribute("errorMessage" , "올바른 요청이 아닙니다.");
            return "login";
        }

        model.addAttribute("successMessage","회원가입이 완료되었습니다.");
        return "login";
    }

    //중복체크
    @GetMapping("/api/check-username")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.getUserByUsername(username) != null;
        return Collections.singletonMap("exists", exists);
    }

    @GetMapping("/api/check-email")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email) || managerService.existsByEmail(email);

        return Collections.singletonMap("exists", exists);
    }

    @GetMapping("/api/check-customerCode-signup")
    @ResponseBody
    public Map<String, Boolean> checkCustomerCodeForSignup(@RequestParam String customerCode) {
        // 고객코드가 존재하고 활성화되어 있는지 확인
        boolean customerExists = userService.isCustomerCodeActive(customerCode);
        
        // 고객코드로 이미 USER 역할의 사용자가 있는지 확인
        boolean userExists = userService.existsByCustomerCode(customerCode);
        
        // 고객코드가 존재하지 않거나 이미 사용자가 있으면 중복으로 처리
        boolean exists = !customerExists || userExists;
        
        return Collections.singletonMap("exists", exists);
    }

    //시큐리티 로그아웃 후 재 로그인시 에러 확인 후 처리
    @GetMapping("/error")
    public String errorPage(HttpServletRequest request, Model model) {
        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (exceptionObj == null) {
            // Spring Security 로그인 실패 예외 처리
            exceptionObj = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            if (exceptionObj != null && exceptionObj instanceof Throwable) {
                Throwable exception = (Throwable) exceptionObj;
                System.err.println("Spring Security 예외 클래스: " + exception.getClass().getName());
                System.err.println("Spring Security 예외 메시지: " + exception.getMessage());
                exception.printStackTrace();

                model.addAttribute("errorMessage", exception.getMessage());
                // 예외 세션에서 제거 (재로그인 시 같은 메시지 반복 방지)
                request.getSession().removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            } else {
                return "redirect:/inquiryList"; // 에러없으면 다음화면으로 이동
            }
        } else if (exceptionObj instanceof Throwable) {
            Throwable exception = (Throwable) exceptionObj;
            System.err.println("에러 클래스: " + exception.getClass().getName());
            System.err.println("에러 메시지: " + exception.getMessage());
            exception.printStackTrace();
            model.addAttribute("errorMessage", exception.getMessage());
        }

        return "login";
    }

    @PostMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable Long id,
                            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                            @RequestParam(value = "search", required = false) String search,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.softDeleteUser(id); // deleteFlag true로 변경
            redirectAttributes.addFlashAttribute("successMessage", "담당자가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "담당자 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        return "redirect:/console";
    }

    @PostMapping("/user/restore/{id}")
    public String restoreUser(@PathVariable Long id,
                             @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                             @RequestParam(value = "search", required = false) String search,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.restoreUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "담당자가 성공적으로 복원되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "담당자 복원 중 오류가 발생했습니다: " + e.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        return "redirect:/console";
    }

    // 내정보 회원정보수정
    @PostMapping("/user/updateMyInfo")
    public String updateMyInfo(@ModelAttribute UserDto userDto,
                              @RequestParam(required = false) String newPassword,
                              @RequestParam(required = false) String confirmPassword,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            // 비밀번호 변경 요청이 없으면 이름, 전화번호, 이메일만 변경
            if (newPassword == null || newPassword.isBlank()) {
                userService.updateUserInfoOnly(userDto);
                redirectAttributes.addFlashAttribute("successMessage", "내 정보가 성공적으로 수정되었습니다.");
            } else {
                // 비밀번호 변경 요청이 있으면 기존 로직 수행
                userService.updateUserWithPassword(userDto, newPassword, confirmPassword);
                redirectAttributes.addFlashAttribute("successMessage", "내 정보가 성공적으로 수정되었습니다.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/myInfo";
    }

    @PostMapping("/user/checkPassword")
    @ResponseBody
    public ResponseEntity<?> checkPassword(@RequestParam String currentPassword) {
        User user = getCurrentUser();
        boolean matches = passwordEncoder.matches(currentPassword, user.getPassword());
        if (matches) {
            return ResponseEntity.ok().body(Collections.singletonMap("valid", true));
        } else {
            return ResponseEntity.ok().body(Collections.singletonMap("valid", false));
        }
    }
}
