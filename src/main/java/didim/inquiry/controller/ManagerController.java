//package didim.inquiry.controller;
//
//import didim.inquiry.controller.absClass.BaseController;
//import didim.inquiry.domain.Manager;
//import didim.inquiry.domain.User;
//import didim.inquiry.dto.ManagerDto;
//import didim.inquiry.security.SecurityUtil;
//import didim.inquiry.service.ManagerService;
//import didim.inquiry.service.UserService;
//import org.springframework.dao.DataAccessException;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.Map;
//
//@Controller
//public class ManagerController extends BaseController {
//
//    private final ManagerService managerService;
//    private final UserService userService;
//
//    public ManagerController(ManagerService managerService, UserService userService) {
//        this.managerService = managerService;
//        this.userService = userService;
//    }
//
//    @PostMapping("/addManager")
//    public String addManager(
//            ManagerDto managerDto,
//            RedirectAttributes redirectAttributes) {
//        try {
//            User findUser = getCurrentUser();
//
//            // 유저 연결
//            managerDto.setUser(findUser);
//            // 관리자 저장
//            managerService.addManager(managerDto);
//
//            redirectAttributes.addFlashAttribute("successMessage","관리자가 성공적으로 추가되었습니다.");
//            return "redirect:/admin/console";
//
//        } catch (UsernameNotFoundException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/login";
//        } catch (Exception e) {
//            System.err.println("addManager Error : " + e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", "서버 오류가 발생했습니다.");
//            return "redirect:/admin/console";
//        }
//    }
//
//    @PostMapping("/addManagerApi")
//    public ResponseEntity<?> addManager(@RequestBody ManagerDto managerDto) {
//        System.out.println("수신된 JSON: " + managerDto);
//        System.out.println("name: " + managerDto.getManagerName());
//        System.out.println("tel: " + managerDto.getManagerTel());
//        System.out.println("email: " + managerDto.getManagerEmail());
//
//        try {
//            User findUser = getCurrentUser();
//
//            // 유효성 검사
//            if (managerDto.getManagerName() == null || managerDto.getManagerTel() == null || managerDto.getManagerEmail() == null) {
//                return ResponseEntity.badRequest().body("모든 필드를 입력해야 합니다.");
//            }
//
//            managerDto.setUser(findUser);
//            Manager manager = managerService.addManager(managerDto);
//            System.out.println("저장된 manager: " + manager.getName());
//
//            ManagerDto responseDto = new ManagerDto();
//            responseDto.setManagerId(manager.getId());
//            responseDto.setManagerName(manager.getName());
//            responseDto.setManagerTel(manager.getTel());
//            responseDto.setManagerEmail(manager.getEmail());
//            responseDto.setUser(manager.getUser());
//
//            return ResponseEntity.ok(responseDto);
//        } catch (UsernameNotFoundException e) {
//            return ResponseEntity.status(404).body("유저를 찾을 수 없습니다.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("관리자 추가 중 서버 오류가 발생했습니다.");
//        }
//    }
//
//    @PostMapping("/manager/delete/{id}")
//    public String deleteManager(
//            @PathVariable("id") Long managerId,
//            @RequestParam(value = "page" , defaultValue = "0") Long page,
//            @RequestParam(value = "size", defaultValue = "10") Long size,
//            @RequestParam(value = "search" , required = false) String search,
//            RedirectAttributes redirectAttributes) {
//        try {
//            // 유저 조회
//            User findUser = getCurrentUser();
//
//            // 관리자 조회 및 삭제
//            Manager manager = managerService.getManagerById(managerId);
//            // 해당관리자의 userid가 현재 로그인한 유저와 일치할 경우 삭제 처리
//            if (findUser.getId().equals(manager.getUser().getId())){
//                managerService.deleteManagerById(managerId);
//            }
//
//            // 응답
//            redirectAttributes.addFlashAttribute("successMessage","관리자가 정상적으로 삭제 되었습니다.");
//            return "redirect:/admin/console?page=" + page + "&size=" + size + "&search=" + search;
//        } catch (UsernameNotFoundException e) {
//            System.err.println("User Name Not Found : " + e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage",e.getMessage());
//            return "redirect:/login";
//        } catch (Exception e) {
//            System.err.println("Error : " + e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage","알수없는 문제가 발생했습니다.");
//            return "redirect:/admin/console";
//        }
//    }
//
//    @PostMapping("/manager/update")
//    @ResponseBody
//    public ResponseEntity<?> updateManager(@RequestBody ManagerDto managerDto,
//                                           @RequestParam("id") Long id){
//        System.out.println(managerDto.getManagerId() + " / " + id);
//        //dto의 id , path의 id가 일치하는지 검증
//        if(!id.equals(managerDto.getManagerId())){
//            Map<String , String> error = Map.of("message","Path ID와s 데이터의 ID가 일치하지 않습니다.");
//            return ResponseEntity.badRequest().body(error);
//        }
//
//        try{
//            String username = SecurityUtil.getCurrentUsername();
//
//            // 로그인 안 된 경우
//            if (username == null) {
//                Map<String, String> error = Map.of("message", "로그인이 필요합니다.");
//                return ResponseEntity.status(401).body(error);
//            }
//
//            // 유저 조회
//            User findUser = userService.getUserByUsername(username);
//            if(findUser == null){
//                Map<String, String> error = Map.of("message", "계정정보가 존재하지 않습니다");
//                return ResponseEntity.status(404).body(error);
//            }
//
//            // 매니저 조회
//            Manager findManager = managerService.getManagerById(id);
//            if(findManager == null){
//                Map<String, String> error = Map.of("message", "매니저 정보가 존재하지 않습니다.");
//                return ResponseEntity.status(404).body(error); // 404에러 응답
//            }
//
//            // 로그인한 유저가 매니저를 수정할 권한이 있는지 검증
//            if (!findManager.getUser().getId().equals(findUser.getId())){
//                Map<String, String> error = Map.of("message", "매니저 정보를 수정할 권한이 없습니다.");
//                return ResponseEntity.status(403).body(error);
//            }
//
//            // 매니저 업데이트
//            ManagerDto updatedManager = managerService.updateManager(managerDto);
//
//            // 업데이트 성공 시
//            Map<String,Object> response = Map.of(
//                    "message","successUpdateManager",
//                    "data",updatedManager
//            );
//
//            return ResponseEntity.ok(response);
//
//        }catch (IllegalArgumentException e){
//            // 서비스단 비즈니스 로직 예외 처리
//            Map<String , String> error = Map.of("message",e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        }catch (DataAccessException e){
//            // 데이터베이스 관련 예외 처리
//            Map<String , String> error = Map.of("message","데이터베이스 오류발생");
//            return ResponseEntity.status(500).body(error);
//        }catch (Exception e){
//            //기타 예상치 못한 예외 처리
//            System.err.println("매니저 업데이트 중 오류 발생 : " + e.getMessage());
//            Map<String , String> error = Map.of("message" , "서버 내부 오류가 발생했습니다.");
//            return ResponseEntity.status(500).body(error);
//        }
//    }
//}