package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.ProjectService;
import didim.inquiry.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class ProjectController extends BaseController {

    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    //프로젝트 생성
    @PostMapping("/addProject")
    public String addProject(
            ProjectDto projectDto,
            RedirectAttributes redirectAttributes){

        try {
            User findUser = getCurrentUser();

            // 프로젝트 유저 Id 세팅 후 저장
            projectDto.setUser(findUser);
            projectService.saveProject(projectDto);

            // 응답
            redirectAttributes.addFlashAttribute("successMessage","프로젝트가 성공적으로 추가 되었습니다.");
            return "redirect:/admin/projectList";

        } catch (UsernameNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage",e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("addProject Error : " + e.getMessage() );
            redirectAttributes.addFlashAttribute("errorMessage","서버 오류가 발생했습니다.");
            return "redirect:/admin/projectList";
        }
    }

    //프로젝트 수정
    @PostMapping("project/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProject(@RequestBody ProjectDto projectDto , @PathVariable("id") Long id){
        if (!id.equals(projectDto.getProjectId())){
            Map<String , String> error = Map.of("message","Path ID와 데이터의 ID가 일치하지 않습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        try{
            //Base 추상클래스를 생성해 상속받아 추상클래스 메서드로 로그인한 유저정보 가져오기
            User user = getCurrentUser();
            projectDto.setUser(user);

            ProjectDto updateProject = projectService.updateProject(projectDto);


            //프로젝트 정보 전달
            return ResponseEntity.ok().body(updateProject);

        }catch (UsernameNotFoundException e){
            System.err.println("유저정보없음 : " + e.getMessage());
            Map<String , String> error = Map.of("message" , e.getMessage());
            return ResponseEntity.status(404).body(error);
        }catch (IllegalArgumentException e){
            System.err.println("에러 : " + e.getMessage());
            Map<String , String> error = Map.of("message" , e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    //프로젝트 삭제
    @PostMapping("/project/delete/{id}")
    public String deleteProject(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        System.out.println("id : " + id);
        try {
            // Base 추상클래스를 생성해 상속받아 추상클래스 메서드로 로그인한 유저정보 가져오기
            User user = getCurrentUser();

            projectService.deleteProjectById(id);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 성공적으로 삭제되었습니다.");
            return "redirect:/admin/projectList";
        } catch (UsernameNotFoundException e) {
            System.err.println("유저정보없음 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "유저 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            System.err.println("에러 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다.");
            return "redirect:/admin/projectList";
        }
    }
}
