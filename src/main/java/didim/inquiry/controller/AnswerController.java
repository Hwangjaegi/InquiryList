package didim.inquiry.controller;

import didim.inquiry.security.AuthenticationHelper;
import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import didim.inquiry.service.AnswerService;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
public class AnswerController {

    private final InquiryService inquiryService;
    private final AnswerService answerService;
    private final EmailService emailService;
    private final AuthenticationHelper authenticationHelper;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Value("${file.upload}")
    private String uploadDir;

    public AnswerController(InquiryService inquiryService, AnswerService answerService, EmailService emailService, AuthenticationHelper authenticationHelper) {
        this.inquiryService = inquiryService;
        this.answerService = answerService;
        this.emailService = emailService;
        this.authenticationHelper = authenticationHelper;
    }

    // 답글용 임시 이미지 업로드
    @PostMapping("/uploadAnswerTempImage")
    @ResponseBody
    public Map<String, String> uploadAnswerTempImage(@RequestParam("image") MultipartFile file, HttpServletRequest request) throws IOException {
        // JWT 토큰으로 사용자 인증 확인
        User currentUser = authenticationHelper.getCurrentUserFromToken(request);
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
            User findUser = authenticationHelper.getCurrentUserFromToken(request);
            answer.setUser(findUser);

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
                    }
                } catch (Exception e) {
                    System.err.println("이미지 URL 파싱 오류: " + e.getMessage());
                }
            }
            
            // 답글에 temp 이미지가 있다면 posts로 이동
            content = answer.getContent();
            if (content != null && content.contains("/temp/")) {
                // temp 이미지 발견! 이동 처리 시작
                String updatedContent = moveImageFromTempToPosts(content);
                answer.setContent(updatedContent);
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
                isWriter = saveAnswer.getUser().getId().equals(saveAnswer.getInquiry().getWriter().getId());
            }

            boolean isManager = saveAnswer.getUser().getRole() != null && saveAnswer.getUser().getRole().equals("ADMIN");
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

                // 파일 이동
                File oldFile = new File(oldFilePath);
                File newFile = new File(newFilePath);
                if (oldFile.exists()) {
                    Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("파일 이동 완료!");

                    // 새로운 경로로 src 업데이트
                    String newSrc = imgSrc.replace("/temp/", "/posts/");
                    updatedContent = new StringBuilder(updatedContent.toString().replace(imgSrc, newSrc));
                    System.out.println("경로 업데이트: " + imgSrc + " -> " + newSrc);
                } else {
                    System.err.println("파일이 존재하지 않습니다: " + oldFilePath);
                }
            } else {
                System.err.println("temp 이미지가 아닙니다: " + imgSrc);
            }
        }

        return updatedContent.toString();
    }
}
