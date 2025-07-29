package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.UserService;
import didim.inquiry.security.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ManagerController extends BaseController {

    private final ManagerService managerService;
    private final UserService userService;

    public ManagerController(ManagerService managerService, UserService userService) {
        this.managerService = managerService;
        this.userService = userService;
    }

    // 담당자 추가 API
    @PostMapping("/api/addManager")
    public ResponseEntity<?> addManager(@RequestBody Manager manager) {
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
            User currentUser = getCurrentUser();
            
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
            response.put("success", false);
            response.put("message", "담당자 추가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/check-email-manager")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        Map<String , Boolean> response = new HashMap<>();
            System.out.println("manager : " + managerService.existsByEmail(email));
            System.out.println("user : " + userService.existsByEmail(email));
        if (managerService.existsByEmail(email) || userService.existsByEmail(email)){
            response.put("exists",true);
        }else{
            response.put("exists",false);
        }

        return response;
    }
}
