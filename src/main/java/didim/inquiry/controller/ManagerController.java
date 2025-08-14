package didim.inquiry.controller;

import didim.inquiry.common.BaseController;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ManagerController extends BaseController {

    private final ManagerService managerService;
    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;

    public ManagerController(ManagerService managerService, UserService userService, AuthenticationHelper authenticationHelper) {
        this.managerService = managerService;
        this.userService = userService;
        this.authenticationHelper = authenticationHelper;
    }

    // 담당자 추가 API
    @PostMapping("/api/addManager")
    public ResponseEntity<?> addManager(@RequestBody Manager manager,
                                        HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 요청 데이터 추출
            String name = manager.getName();
            String tel = manager.getTel();
            String email = manager.getEmail();
            
            // 필수 필드 검증
            if (name == null || name.trim().isEmpty() ||
                tel == null || tel.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "모든 필드를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 이메일 중복 확인
            if (managerService.existsByEmail(email) || userService.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "이미 사용중인 이메일입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 현재 로그인한 사용자의 ID 가져오기
            User currentUser = authenticationHelper.getCurrentUserFromToken(request);
            System.out.println("currentuser 이름 : " + currentUser.getName());
            
            // 새 매니저 생성
            Manager newManager = managerService.createManager(name, tel, email, currentUser);
            
            // 성공 응답
            response.put("success", true);
            response.put("message", "담당자가 성공적으로 추가되었습니다.");
            response.put("manager", Map.of(
                "id", newManager.getId(),
                "name", newManager.getName(),
                "tel", newManager.getTel(),
                "email", newManager.getEmail()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("담당자 추가중 에러 : " + e.getMessage());
            response.put("success", false);
            response.put("message", "담당자 추가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/check-email-manager")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email, 
                                          @RequestParam(required = false) Long excludeManagerId) {
        Map<String, Boolean> response = new HashMap<>();
        
        boolean exists = false;
        
        // Manager 테이블에서 중복 확인
        if (excludeManagerId != null) {
            // 수정 시: 특정 담당자 제외하고 중복 확인
            exists = managerService.existsByEmailExcludingManager(email, excludeManagerId);
        } else {
            // 추가 시: 모든 담당자 대상으로 중복 확인
            exists = managerService.existsByEmail(email);
        }
        
        // User 테이블에서도 중복 확인
        if (!exists) {
            exists = userService.existsByEmail(email);
        }
        
        response.put("exists", exists);
        return response;
    }

    // 담당자 수정 API
    @PostMapping("/manager/update")
    @ResponseBody
    public ResponseEntity<?> updateManager(@RequestBody Manager manager,
                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 필수 필드 검증
            if (manager.getId() <= 0 || manager.getName() == null || manager.getName().trim().isEmpty() ||
                manager.getTel() == null || manager.getTel().trim().isEmpty() ||
                manager.getEmail() == null || manager.getEmail().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "모든 필드를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 현재 로그인한 사용자 확인
            User currentUser = authenticationHelper.getCurrentUserFromToken(request);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 담당자 수정
            Manager updatedManager = managerService.updateManager(manager.getId(), manager.getName(), 
                                                               manager.getTel(), manager.getEmail(), currentUser);
            
            // 성공 응답
            response.put("success", true);
            response.put("message", "담당자 정보가 성공적으로 수정되었습니다.");
            response.put("manager", Map.of(
                "id", updatedManager.getId(),
                "name", updatedManager.getName(),
                "tel", updatedManager.getTel(),
                "email", updatedManager.getEmail()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "담당자 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 담당자 삭제 API
    @PostMapping("/manager/delete/{id}")
    public String deleteManager(@PathVariable Long id, 
                               @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                               @RequestParam(value = "search", required = false) String search,
                               RedirectAttributes redirectAttributes) {
        try {
            // 현재 로그인한 사용자 확인
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            // 담당자 삭제
            managerService.deleteManager(id, currentUser);
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

    // 담당자 복원 API
    @PostMapping("/manager/restore/{id}")
    public String restoreManager(@PathVariable Long id,
                                @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                @RequestParam(value = "search", required = false) String search,
                                RedirectAttributes redirectAttributes) {
        try {
            // 현재 로그인한 사용자 확인
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            // 담당자 복원
            managerService.restoreManager(id, currentUser);
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
}
