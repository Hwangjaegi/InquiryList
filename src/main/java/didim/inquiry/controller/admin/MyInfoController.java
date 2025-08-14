package didim.inquiry.controller.admin;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 개인정보 관리 컨트롤러
 * 내 정보 조회 기능을 처리합니다.
 */
@Controller
public class MyInfoController{

    private final AuthenticationHelper authenticationHelper;

    public MyInfoController(AuthenticationHelper authenticationHelper) {
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/myInfo")
    public String myInfo(Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        System.out.println("요청전달");
        try {
            User user = authenticationHelper.getCurrentUserFromToken(request);
            model.addAttribute("user", user);
            model.addAttribute("role", user.getRole());
            return "page/myInfo";
        } catch (UsernameNotFoundException e) {
            System.err.println("User Not Found Error : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }
}
