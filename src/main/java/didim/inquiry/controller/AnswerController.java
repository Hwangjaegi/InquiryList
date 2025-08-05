package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.AnswerService;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.UserService;
import didim.inquiry.service.EmailService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import didim.inquiry.security.JwtTokenProvider;

@Controller
public class AnswerController extends BaseController {

    private final UserService userService;
    private final InquiryService inquiryService;
    private final AnswerService answerService;
    private final EmailService emailService;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Value("${file.upload}")
    private String uploadDir;

    public AnswerController(UserService userService, InquiryService inquiryService, AnswerService answerService, EmailService emailService) {
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.answerService = answerService;
        this.emailService = emailService;
    }


    @PostMapping("/answerWrite")
    public String answerWrite(Answer answer, Model model , RedirectAttributes redirectAttributes){
        System.out.println("문의번호 : " + answer.getInquiry().getId());

        try {
            User findUser = getCurrentUser();
            //댓글 작성자 id 답글 객체에 저장
            answer.setUser(findUser);

            answerService.saveAnswer(answer);

            //대상 문의에 상태 변경
            Long inquiryId = answer.getInquiry().getId();
            Inquiry inquiry =  inquiryService.getInquiryById(inquiryId).orElseThrow(() -> new IllegalArgumentException("해당 문의 없음"));
            inquiry.setStatus("답변완료");
            inquiryService.saveInquiry(inquiry);

            redirectAttributes.addFlashAttribute("successMessage" , "답변이 성공적으로 등록되었습니다.");
        }catch (Exception e){
            System.err.println("문의 저장 중 오류 발생 : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage","문의 저장 중 오류가 발생했습니다. 다시 시도해주세요!");
        }
        return "redirect:/inquiryList";
    }

    // 답글용 임시 이미지 업로드
    @PostMapping("/uploadAnswerTempImage")
    @ResponseBody
    public Map<String, String> uploadAnswerTempImage(@RequestParam("image") MultipartFile file, HttpServletRequest request) throws IOException {
        // JWT 토큰으로 사용자 인증 확인
        User currentUser = getCurrentUserFromToken(request);
        if (currentUser == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        if (file.isEmpty()) {
            throw new RuntimeException("파일이 없습니다.");
        }
        
        // 저장할 파일명 생성 (예: timestamp + 랜덤값)
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

        // temp 디렉토리에 저장 , 디렉토리 없으면 생성
        String tempDir = uploadDir + "/temp/";
        File directory = new File(tempDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File saveFile = new File(tempDir + filename);
        file.transferTo(saveFile);

        // 웹 접근경로
        String webUrl = "/temp/" + filename;

        Map<String, String> result = new HashMap<>();
        result.put("url", webUrl);
        return result;
    }

    @PostMapping("/answerWriteAjax")
    @ResponseBody
    public Map<String, Object> answerWriteAjax(Answer answer, @RequestParam(value = "imageUrls", required = false) String imageUrlsJson, HttpServletRequest request) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            User findUser = getCurrentUserFromToken(request);
            answer.setUser(findUser);
            
            // 디버깅: 콘텐츠 확인
            System.out.println("=== 답글 작성 디버깅 ===");
            System.out.println("원본 콘텐츠: " + answer.getContent());
            System.out.println("이미지 URL JSON: " + imageUrlsJson);
            
            // 이미지 URL들을 콘텐츠에 추가
            String content = answer.getContent();
            if (imageUrlsJson != null && !imageUrlsJson.isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<String> imageUrls = objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
                    
                    if (!imageUrls.isEmpty()) {
                        // 이미지 HTML 생성
                        StringBuilder imageHtml = new StringBuilder();
                        for (String imageUrl : imageUrls) {
                            imageHtml.append("<img src=\"").append(imageUrl).append("\" class=\"answer-img\" \"><br>");
                        }
                        
                        // 텍스트 콘텐츠와 이미지 HTML 결합
                        String newContent = content + (content != null && !content.trim().isEmpty() ? "\n\n" : "") + imageHtml.toString();
                        answer.setContent(newContent);
                        
                        System.out.println("이미지 추가 후 콘텐츠: " + answer.getContent());
                    }
                } catch (Exception e) {
                    System.err.println("이미지 URL 파싱 오류: " + e.getMessage());
                }
            }
            
            // 답글에 temp 이미지가 있다면 posts로 이동
            content = answer.getContent();
            if (content != null && content.contains("/temp/")) {
                System.out.println("temp 이미지 발견! 이동 처리 시작...");
                String updatedContent = moveImageFromTempToPosts(content);
                answer.setContent(updatedContent);
                System.out.println("이동 후 콘텐츠: " + answer.getContent());
            } else {
                System.out.println("temp 이미지가 없습니다.");
                if (content != null) {
                    System.out.println("콘텐츠 내용: [" + content + "]");
                }
            }
            
            answerService.saveAnswer(answer);
            // 답글 저장 후, DB에서 다시 조회(연관객체 포함, fetch join)
            Answer saveAnswer = answerService.getAnswerWithInquiryAndWriterById(answer.getId()).orElse(answer);

            Long inquiryId = saveAnswer.getInquiry().getId();
            Inquiry inquiry = inquiryService.getInquiryById(inquiryId).orElseThrow(() -> new IllegalArgumentException("해당 문의 없음"));;
            inquiry.setStatus("답변완료");
            inquiryService.saveInquiry(inquiry);

            // 답변 작성자에게 이메일 알림 발송
            try {
                emailService.sendAnswerNotification(saveAnswer);
            } catch (Exception e) {
                System.err.println("답변 알림 이메일 발송 실패: " + e.getMessage());
                // 이메일 발송 실패는 답변 등록에 영향을 주지 않도록 함
            }

            Context context = new Context();
            context.setVariable("answer", saveAnswer);
            boolean isWriter = false;
            if (saveAnswer.getInquiry() != null && saveAnswer.getInquiry().getWriter() != null) {
                System.out.println("saved.getUser().getId(): " + saveAnswer.getUser().getId());
                System.out.println("saved.getInquiry().getWriter().getId(): " + saveAnswer.getInquiry().getWriter().getId());
                isWriter = saveAnswer.getUser().getId().equals(saveAnswer.getInquiry().getWriter().getId());
                System.out.println("여기들어오니? isWriter: " + isWriter);
            }
            boolean isManager = saveAnswer.getUser().getRole() != null && saveAnswer.getUser().getRole().equals("ADMIN");
            System.out.println("isWriter : " + isWriter + " / isManager : " + isManager);
            context.setVariable("isWriter", isWriter);
            context.setVariable("isManager", isManager);
            // inquiry 객체도 context에 추가하여 매니저 정보에 접근할 수 있도록 함
            context.setVariable("inquiry", saveAnswer.getInquiry());
            String answerHtml = templateEngine.process("inquiry/answerFragment", context);
            result.put("success", true);
            result.put("answerHtml", answerHtml);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    

    
    // temp폴더에서 posts폴더로 이동처리 메서드
    private String moveImageFromTempToPosts(String content) throws IOException {
        System.out.println("=== 이미지 이동 처리 시작 ===");
        System.out.println("처리할 콘텐츠: " + content);
        
        // 정규식을 사용해 img 태그의 src 속성 추출
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"'](.*?)['\"]");
        Matcher matcher = pattern.matcher(content);
        StringBuilder updatedContent = new StringBuilder(content);

        // 파일 시스템 경로
        String uploadPath = uploadDir;
        String tempPath = uploadPath + "/temp/";
        String postsPath = uploadPath + "/posts/";
        
        System.out.println("업로드 경로: " + uploadPath);
        System.out.println("temp 경로: " + tempPath);
        System.out.println("posts 경로: " + postsPath);

        // posts 폴더가 없으면 생성
        File postsDir = new File(postsPath);
        if (!postsDir.exists()) {
            postsDir.mkdirs();
        }

        while (matcher.find()) {
            String imgSrc = matcher.group(1);
            System.out.println("발견된 이미지 src: " + imgSrc);
            
            if (imgSrc.contains("/temp/")) {
                System.out.println("temp 이미지 발견! 이동 처리...");
                
                // 파일명 추출
                String fileName = imgSrc.substring(imgSrc.lastIndexOf("/") + 1);
                String oldFilePath = tempPath + fileName;
                String newFilePath = postsPath + fileName;
                
                System.out.println("파일명: " + fileName);
                System.out.println("이전 경로: " + oldFilePath);
                System.out.println("새 경로: " + newFilePath);

                // 파일 이동
                File oldFile = new File(oldFilePath);
                File newFile = new File(newFilePath);
                if (oldFile.exists()) {
                    System.out.println("파일 존재 확인됨. 이동 시작...");
                    Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("파일 이동 완료!");

                    // 새로운 경로로 src 업데이트
                    String newSrc = imgSrc.replace("/temp/", "/posts/");
                    updatedContent = new StringBuilder(updatedContent.toString().replace(imgSrc, newSrc));
                    System.out.println("경로 업데이트: " + imgSrc + " -> " + newSrc);
                } else {
                    System.out.println("파일이 존재하지 않습니다: " + oldFilePath);
                }
            } else {
                System.out.println("temp 이미지가 아닙니다: " + imgSrc);
            }
        }

        return updatedContent.toString();
    }

    // JWT 토큰에서 사용자 정보 가져오기
    private User getCurrentUserFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String usernameWithCustomerCode = jwtTokenProvider.getUsernameFromToken(token);
            
            System.out.println("JWT 토큰에서 추출한 사용자명: " + usernameWithCustomerCode);
            
            // username|customerCode 형태인 경우
            if (usernameWithCustomerCode != null && usernameWithCustomerCode.contains("|")) {
                String[] parts = usernameWithCustomerCode.split("\\|");
                if (parts.length == 2) {
                    String username = parts[0];
                    String customerCode = parts[1];
                    return userService.getUserByUsernameAndCustomerCode(username, customerCode);
                }
            }
            
            // username만 있는 경우 (기존 방식)
            return userService.getUserByUsername(usernameWithCustomerCode);
        }
        return getCurrentUser(); // JWT 토큰이 없으면 세션 기반 인증 사용
    }

    // 요청에서 JWT 토큰 추출
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. URL 파라미터에서 토큰 확인
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam;
        }

        return null;
    }
}
