package didim.inquiry.controller;

import didim.inquiry.domain.User;
import didim.inquiry.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(){
        return "login";
    }


    //로그인 요청 처리 (시큐리티 Config 처리)
    @GetMapping("/login")
    public String login(@RequestParam(value = "error" , required = false) String error,
                        Model model,
                        HttpServletRequest request){
        if (error != null){
            System.err.println(error);
            model.addAttribute("loginErrorMessage" , "일치하는 계정 정보가 존재하지 않습니다");
        }
        return "login";
    }

    //회원가입 요청 처리
    @PostMapping("/signup")
    public String joinUser(User user , Model model){

        //고객코드가 존재하는지 확인 + 고객코드가 같은 유저중 아이디가 같은 경우가 존재하는지 확인 후 가입처리
        boolean success = userService.signUpUser(user);

        if (!success){
            model.addAttribute("errorMessage" , "고객코드가 유효하지 않거나 존재하는 아이디입니다.");
            return "login";
        }

        model.addAttribute("successMessage","회원가입이 완료되었습니다.");
        return "login";
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
                System.out.println("Spring Security 예외 클래스: " + exception.getClass().getName());
                System.out.println("Spring Security 예외 메시지: " + exception.getMessage());
                exception.printStackTrace();

                model.addAttribute("errorMessage", exception.getMessage());
                // 예외 세션에서 제거 (재로그인 시 같은 메시지 반복 방지)
                request.getSession().removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            } else {
                return "redirect:/inquiryList"; // 에러없으면 다음화면으로 이동
            }
        } else if (exceptionObj instanceof Throwable) {
            Throwable exception = (Throwable) exceptionObj;
            System.out.println("에러 클래스: " + exception.getClass().getName());
            System.out.println("에러 메시지: " + exception.getMessage());
            exception.printStackTrace();
            model.addAttribute("errorMessage", exception.getMessage());
        }

        return "login";
    }

}
