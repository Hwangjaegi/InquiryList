package didim.inquiry.controller;

import ch.qos.logback.core.joran.conditional.IfAction;
import didim.inquiry.domain.*;
import didim.inquiry.dto.ManagerDto;
import didim.inquiry.dto.ProjectDto;
import didim.inquiry.dto.SearchInquiryDto;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.ManagerService;
import didim.inquiry.service.ProjectService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class InquiryController {

    private final InquiryService inquiryService;
    private final UserService userService;
    private final ProjectService projectService;
    private final ManagerService managerService;
    @Value("${file.upload}")
    private String uploadDir;

    public InquiryController(InquiryService inquiryService, UserService userService, ProjectService projectService, ManagerService managerService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
        this.projectService = projectService;
        this.managerService = managerService;
    }

    @GetMapping("/inquiryList")
    public String inquiryList(Model model, HttpServletRequest request,
                              @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== 디버깅 정보 ===");
        System.out.println("요청 시간: " + new Date());
        System.out.println("세션 ID: " + request.getSession().getId());
        System.out.println("요청 URL: " + request.getRequestURL());
        System.out.println("Authentication: " + SecurityContextHolder.getContext().getAuthentication());

//        System.out.println("Referer:" + request.getHeader("Referer"));
//        System.out.println("User-Agent: " + request.getHeader("User-Agent"));
//        System.out.println("X-Requested-With : " + request.getHeader("X-Request-With"));
//        Thread.dumpStack();

        String username = SecurityUtil.getCurrentUsername();
        System.out.println("SecurityUtil username: " + username);

        // 1. 아이디가 없을 경우 로그인 화면으로 이동
        if (username == null) {
            return "redirect:/login?error=true";
        }

        // 2. 아이디가 있을 경우 해당 아이디의 권한 가져오기
        User findUser = userService.getUserByUsername(username);
        String role = findUser.getRole();

        // * 추가사항(2025-07-10) : 처음 리스트 불러올때 답변 대기중 , 답변완료인 데이터만 조회
        SearchInquiryDto searchInquiryDto = new SearchInquiryDto();
        if (role.equals("ADMIN")) {
            searchInquiryDto.setStatus(Arrays.asList("답변 대기중", "답변완료"));
        }

        // 3. 페이지당 10개 문의 조회 (페이징 적용) , 답변상태(답변 대기중 , 답변완료) 조회
        Pageable pageable = PageRequest.of(page, 10); // 페이지당 10개
        Page<Inquiry> inquiries;
        inquiries = inquiryService.getInquiryBySearch(searchInquiryDto, role, username, pageable);

        // 4. 문의 내용 줄바꿈 기호 <br>로 변경
        inquiries.forEach(inquiry -> {
            String content = inquiry.getContent();
            if (content != null) {
                inquiry.setContent(content.replaceAll("\n", "<br>"));
            }

            if (inquiry.getAnswers() != null) {
                inquiry.getAnswers().forEach(answer -> {
                    String answerContent = answer.getContent();
                    answer.setContent(answerContent.replaceAll("\n", "<br>"));
                });

                // 5. 답글을 작성일 순서로 정렬
                inquiry.getAnswers().sort(Comparator.comparing(Answer::getRepliedAt));
            }
        });

        System.out.println("인쿼리콘텐츠 : " + inquiries.getContent());
        System.out.println("인쿼리개수 : " + inquiries.getTotalElements());
        System.out.println("인쿼리비었나 : " + inquiries.getContent().isEmpty());


        // 6. 모델에 데이터 추가
        model.addAttribute("answerCount", 1);
        model.addAttribute("inquiries", inquiries);
        // * 기본적으로 답변상태 체크처리를 위해 추가
        model.addAttribute("searchInquiry", searchInquiryDto);
        model.addAttribute("role", role);
        return "inquiry/inquiryList";
    }

    @GetMapping("/inquiryWriteForm")
    public String inquiryWriteForm(Model model, RedirectAttributes redirectAttributes) {
        String username = SecurityUtil.getCurrentUsername();
        User user;
        try {
            user = userService.getUserByUsername(username);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 계정입니다.");
            return "redirect:/login";
        }

        //계정정보 확인 후 해당 유저에 해당하는 관리자 및 프로젝트 리스트 가져오기
        //user_id -> Manager List 가져온다음 -> 드롭다운 표시
        List<Manager> managerList = managerService.getManagerList(user.getId());
        managerList.forEach(manager -> {
            System.out.println("매니저 정보 : " + manager.getName());
        });

        //user_id -> Project List 가져온다음 -> 드롭다운 표시
        List<Project> projectList = projectService.getProjectList(user.getId());
        projectList.forEach(project -> {
            System.out.println("프로젝트 정보 : " + project.getSubject());
        });

        model.addAttribute("managerList", managerList);
        model.addAttribute("projectList", projectList);
        return "inquiry/inquiryWriteForm";
    }


    // 이미지 임시 업로드 - 멀티파트 파일 받음
    @PostMapping("/uploadTempImage")
    @ResponseBody
    public Map<String, String> uploadTempImage(@RequestParam("image") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 없습니다.");
        }
        System.out.println("이미지 Temp 업로드 시작");
        // 저장할 파일명 생성 (예: timestamp + 랜덤값)
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

        //배포시 배포환경에맞게 경로 변경
        String tempDir = uploadDir + "/temp/";
        File directory = new File(tempDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File saveFile = new File(tempDir + filename);
        file.transferTo(saveFile);

        //웹 접근경로
        String webUrl = "/temp/" + filename;

        Map<String, String> result = new HashMap<>();
        result.put("url", webUrl);
        return result; //클라이언트 응답 : "url" : "/temp/파일명"
    }

    @PostMapping("/inquiryWrite")
    public String inquiryWrite(Inquiry inquiry,
                               ManagerDto managerDto,
                               ProjectDto projectDto,
                               RedirectAttributes redirectAttributes) {

        System.out.println("이쪽들어옴!");
        String username = SecurityUtil.getCurrentUsername();

        try {
            //로그인 사용자 유저 정보 확인
            User findUser = userService.getUserByUsername(username);
            //로그인 사용자 id를 inquiey의 fk로 저장
            inquiry.setWriter(findUser);
            inquiry.setCreatedAt(LocalDateTime.now());
            inquiry.setStatus("답변 대기중");

            //관리자 조회
            System.out.println("매니저아이디 : " + managerDto.getManagerId());
            Manager selectedManager = managerService.getSelectedManager(managerDto.getManagerId());
            inquiry.setManager(selectedManager);

            //프로젝트 조회
            Project selectedProject = projectService.getProject(projectDto.getProjectId());
            inquiry.setProject(selectedProject);

            //문의에 이미지가 존재한다면 이미지 경로 변경 및 위치 변경
            String content = inquiry.getContent();
            if (content != null && content.contains("<img")) {
                //inquery의 content내 이미지태그의 임시저장이미지폴더 경로를 실제 저장경로로 변경 및 파일 이동 처리
                String updatedContent = moveImageFromTempToPosts(inquiry.getContent());
                inquiry.setContent(updatedContent);
            }
            //inquiry 레코드 생성
            inquiryService.saveInquiry(inquiry);

            redirectAttributes.addFlashAttribute("successMessage", "문의가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            System.err.println("문의 저장 중 오류 발생 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "문의 저장 중 오류가 발생했습니다. 다시 시도해주세요!");
        }

        return "redirect:/inquiryList";
    }

    //temp폴더에서 posts폴더로 이동처리 메서드
    private String moveImageFromTempToPosts(String content) throws IOException {
        // 정규식을 사용해 img 태그의 src 속성 추출
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"'](.*?)['\"]");
        Matcher matcher = pattern.matcher(content);
        StringBuilder updatedContent = new StringBuilder(content);

        // 파일 시스템 경로
        String uploadPath = uploadDir;
        String tempPath = uploadPath + "/temp/";
        String postsPath = uploadPath + "/posts/";

        // posts 폴더가 없으면 생성
        File postsDir = new File(postsPath);
        if (!postsDir.exists()) {
            postsDir.mkdirs();
        }

        while (matcher.find()) {
            String imgSrc = matcher.group(1);
            if (imgSrc.contains("/temp/")) {
                // 파일명 추출
                String fileName = imgSrc.substring(imgSrc.lastIndexOf("/") + 1);
                String oldFilePath = tempPath + fileName;
                String newFilePath = postsPath + fileName;

                // 파일 이동
                File oldFile = new File(oldFilePath);
                File newFile = new File(newFilePath);
                if (oldFile.exists()) {
                    Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // 새로운 경로로 src 업데이트
                    String newSrc = imgSrc.replace("/temp/", "/posts/");
                    updatedContent = new StringBuilder(updatedContent.toString().replace(imgSrc, newSrc));
                }
            }
        }

        return updatedContent.toString();
    }

    // 관리자 문의 처리완료 시
    @PostMapping("/inquiryComplete")
    @ResponseBody
    public ResponseEntity<?> inquiryComplete(@RequestParam("id") Long inquiryId) {
        try {
            System.out.println("아이디 : " + inquiryId);
            inquiryService.updateInquiryStatus(inquiryId);

            return ResponseEntity.ok().body(Map.of("success", true, "message", "문의가 처리완료 되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 검색 필터 사용
    @GetMapping("/searchInquiries")
    public String searchInquiries(@ModelAttribute SearchInquiryDto searchInquiry,
                                  BindingResult result,
                                  Model model,
                                  @RequestParam(defaultValue = "0") int page) {
        // 1. 로그인한 계정 정보 확인
        String username = SecurityUtil.getCurrentUsername();
        if (username == null) {
            System.out.println("username이 null입니다 - 로그인 페이지로 리다이렉트");
            return "redirect:/login?error=true";
        }

        // 2. 에러 처리 ?
        if (result.hasErrors()) {
            model.addAttribute("inquiries", Page.empty());
            return "inquiry/inquiryList";
        }

        // 3. 로그인한 계정의 권한 조회
        User findUser = userService.getUserByUsername(username);
        String role = findUser.getRole();

        // 4. 페이지당 10개 문의 조회 (페이징 적용)
        Pageable pageable = PageRequest.of(page, 10); // 페이지당 10개
        Page<Inquiry> inquiries = inquiryService.getInquiryBySearch(searchInquiry, role, username, pageable);

        // 5. 문의 내용 줄바꿈 기호 <br>로 변경
        inquiries.forEach(inquiry -> {
            String content = inquiry.getContent();
            if (content != null) {
                inquiry.setContent(content.replaceAll("\n", "<br>"));
            }
            if (inquiry.getAnswers() != null) {
                inquiry.getAnswers().forEach(answer -> {
                    String answerContent = answer.getContent();
                    answer.setContent(answerContent.replaceAll("\n", "<br>"));
                });
                inquiry.getAnswers().sort(Comparator.comparing(Answer::getRepliedAt));
            }
        });

        // 6. 모델에 데이터 추가
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("role", role);
        // 검색 조건을 모델에 추가하여 템플릿에서 재사용
        model.addAttribute("searchInquiry", searchInquiry);
        return "inquiry/inquiryList";
    }
}
