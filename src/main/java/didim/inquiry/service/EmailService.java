package didim.inquiry.service;

import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.domain.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.mail.from}")
    private String mailFrom;

    @Value("${synology.chat.url}")
    private String synologyUrl;

    @Value("${synology.chat.token}")
    private String synologyToken;

    /**
     * Synology Chat으로 메시지 전송
     */
    private void sendSynologyChatMessage(String message) {
        try {
            // 토큰에서 URL 인코딩된 따옴표 제거
            String cleanToken = synologyToken.replace("%22", "");
            
            // URL에 토큰 파라미터 추가
            String fullUrl = synologyUrl + "&token=" + cleanToken;
            
            System.out.println("Synology Chat URL: " + fullUrl);
            
            // 헤더 설정 - UTF-8 인코딩 명시
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept-Charset", "UTF-8");
            
            // 페이로드 생성 - URL 인코딩 사용
            String jsonPayload = createJsonPayload(message);
            String encodedPayload = URLEncoder.encode(jsonPayload, StandardCharsets.UTF_8);
            String payload = "payload=" + encodedPayload;
            
            System.out.println("Synology Chat Payload: " + payload);
            
            // HTTP 요청 생성
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            
            // POST 요청 전송
            ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, request, String.class);
            
            System.out.println("Synology Chat 응답 상태: " + response.getStatusCode());
            System.out.println("Synology Chat 응답 내용: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Synology Chat 메시지 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * JSON 페이로드 생성 (Synology Chat 호환 형식)
     */
    private String createJsonPayload(String message) {
        try {
            // 메시지에서 줄바꿈 문자를 실제 줄바꿈으로 변환
            String formattedMessage = message.replace("\n", "\n");
            
            // 특수문자 이스케이프 처리
            formattedMessage = formattedMessage.replace("\"", "\\\"");
            formattedMessage = formattedMessage.replace("\\", "\\\\");
            
            // Synology Chat 호환 형식으로 페이로드 생성
            String jsonPayload = "{\"text\":\"" + formattedMessage + "\",\"mrkdwn\":true}";
            
            // UTF-8로 인코딩 확인
            System.out.println("원본 메시지: " + message);
            System.out.println("JSON 페이로드: " + jsonPayload);
            
            return jsonPayload;
        } catch (Exception e) {
            System.err.println("JSON 페이로드 생성 실패: " + e.getMessage());
            return "{\"text\":\"메시지 전송 중 오류가 발생했습니다.\"}";
        }
    }

    /**
     * 문의 작성 시 ADMIN 권한 사용자들에게 이메일 발송 및 Synology Chat 알림
     */
    public void sendInquiryNotification(Inquiry inquiry, String company) {
        try {
            // ADMIN 권한을 가진 모든 사용자 조회
            Page<User> adminUsersPage = userService.getUsersByRole(List.of("ADMIN"), Pageable.unpaged());
            List<User> adminUsers = adminUsersPage.getContent();

            if (adminUsers.isEmpty()) {
                System.out.println("ADMIN 권한을 가진 사용자가 없습니다.");
                return;
            }

            // 각 ADMIN 사용자에게 이메일 발송
            for (User adminUser : adminUsers) {
                if (adminUser.getEmail() != null && !adminUser.getEmail().trim().isEmpty()) {
                    sendInquiryNotificationToAdmin(adminUser, inquiry, company);
                }
            }

            // Synology Chat 알림 전송
            sendInquiryNotificationToSynologyChat(inquiry, company);
        } catch (Exception e) {
            System.err.println("이메일 발송 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 ADMIN 사용자에게 문의 알림 이메일 발송
     */
    private void sendInquiryNotificationToAdmin(User adminUser, Inquiry inquiry, String company) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminUser.getEmail());
            message.setSubject("[문의 시스템] 새로운 문의가 등록되었습니다");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = inquiry.getCreatedAt().format(formatter);

            String content = String.format(
                    "[디딤솔루션 문의 접수]\n\n" +
                            "안녕하세요, %s님\n\n" +
                            "새로운 문의가 등록되었습니다.\n\n" +
                            "문의 정보:\n" +
                            "- 티켓번호: %s\n" +
                            "- 회사명: %s\n" +
                            "- 담당자: %s\n" +
                            "- 프로젝트: %s\n" +
                            "- 제목: %s\n" +
                            "- 내용: %s\n" +
                            "- 등록일시: %s\n\n" +
                            "문의내역에서 확인하실 수 있습니다.\n" +
                            "감사합니다.",
                    adminUser.getName(),
                    inquiry.getTickNumber(),
                    company,
                    inquiry.getManager() != null ? inquiry.getManager().getName() : inquiry.getWriter().getName(),
                    inquiry.getProject() != null ? inquiry.getProject().getSubject() : "기타문의",
                    inquiry.getTitle(),
                    inquiry.getContent() != null ? inquiry.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            message.setFrom(mailFrom);
            mailSender.send(message);

            System.out.println("이메일 발송 완료: " + adminUser.getEmail());
        } catch (Exception e) {
            System.err.println("이메일 발송 실패 (" + adminUser.getEmail() + "): " + e.getMessage());
        }
    }

    /**
     * Synology Chat으로 문의 알림 전송
     */
    private void sendInquiryNotificationToSynologyChat(Inquiry inquiry, String company) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = inquiry.getCreatedAt().format(formatter);

            String message = String.format(
                    "[디딤솔루션 문의 접수]\n\n" +
                    "새로운 문의가 등록되었습니다.\n\n" +
                    "문의 정보:\n" +
                    "- 티켓번호:%s\n" +
                    "- 회사명: %s\n" +
                    "- 담당자: %s\n" +
                    "- 프로젝트: %s\n" +
                    "- 제목: %s\n" +
                    "- 내용: %s\n" +
                    "- 등록일시: %s\n\n" +
                    "문의내역에서 확인하실 수 있습니다.",
                    inquiry.getTickNumber(),
                    company,
                    inquiry.getManager() != null ? inquiry.getManager().getName() : inquiry.getWriter().getName(),
                    inquiry.getProject() != null ? inquiry.getProject().getSubject() : "기타문의",
                    inquiry.getTitle(),
                    inquiry.getContent() != null ? inquiry.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            sendSynologyChatMessage(message);
        } catch (Exception e) {
            System.err.println("Synology Chat 문의 알림 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 답변 작성 시 알림 이메일 발송 및 Synology Chat 알림
     * ADMIN이 답변하면 문의 작성자에게, 문의 작성자가 답변하면 ADMIN에게 발송
     */
    public void sendAnswerNotification(Answer answer) {
        try {
            String answerWriterRole = answer.getUser().getRole();

            if ("ADMIN".equals(answerWriterRole)) {
                // ADMIN이 답변한 경우 -> 문의 작성자에게 알림
                sendAnswerNotificationToInquiryWriter(answer);
            } else {
                // 문의 작성자가 답변한 경우 -> ADMIN들에게 알림
                sendAnswerNotificationToAdmins(answer);
            }

            // Synology Chat 알림 전송
            sendAnswerNotificationToSynologyChat(answer);
        } catch (Exception e) {
            System.err.println("답변 알림 이메일 발송 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * ADMIN이 답변했을 때 문의 작성자에게 알림 발송
     */
    private void sendAnswerNotificationToInquiryWriter(Answer answer) {
        try {
            User inquiryWriter = answer.getInquiry().getWriter();
            if (inquiryWriter.getEmail() == null || inquiryWriter.getEmail().trim().isEmpty()) {
                System.err.println("문의 작성자의 이메일이 없습니다.");
                return;
            }


            //담당자 있을시 담당자 이메일 없을시 로그인 유저 이메일 주소로 전송
            SimpleMailMessage message = new SimpleMailMessage();
            String toEmail = answer.getInquiry().getManager() != null ?
                    answer.getInquiry().getManager().getEmail() : inquiryWriter.getEmail();
            message.setTo(toEmail);
            message.setSubject("[문의 시스템] 문의에 답변이 등록되었습니다");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = answer.getRepliedAt().format(formatter);

            String content = String.format(
                    "[디딤솔루션 문의 답변]\n\n" +
                    "안녕하세요, %s님\n\n" +
                    "문의에 답변이 등록되었습니다.\n\n" +
                    "문의 정보:\n" +
                    "- 티켓 번호: %s\n" +
                    "- 문의 제목: %s\n" +
                    "- 답변 작성자: %s\n" +
                    "- 답변 내용: %s\n" +
                    "- 답변 일시: %s\n\n" +
                    "문의목록에서 확인하실 수 있습니다.\n",
                    answer.getInquiry().getManager() != null ? answer.getInquiry().getManager().getName() : inquiryWriter.getName(),
                    answer.getInquiry().getTickNumber(),
                    answer.getInquiry().getTitle(),
                    answer.getUser().getName(),
                    answer.getContent() != null ? answer.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            message.setFrom(mailFrom);
            mailSender.send(message);

            System.out.println("문의 작성자에게 답변 알림 이메일 발송 완료: " + toEmail);
        } catch (Exception e) {
            System.err.println("문의 작성자에게 답변 알림 이메일 발송 실패: " + e.getMessage());
        }
    }

    /**
     * 문의 작성자가 답변했을 때 ADMIN들에게 메일 발송
     */
    private void sendAnswerNotificationToAdmins(Answer answer) {
        try {
            // ADMIN 권한을 가진 모든 사용자 조회
            Page<User> adminUsersPage = userService.getUsersByRole(List.of("ADMIN"), Pageable.unpaged());
            List<User> adminUsers = adminUsersPage.getContent();

            if (adminUsers.isEmpty()) {
                System.out.println("ADMIN 권한을 가진 사용자가 없습니다.");
                return;
            }

            // 각 ADMIN 사용자에게 이메일 발송 , 추후 어드민계정은 추가로 생성할 경우
            for (User adminUser : adminUsers) {
                if (adminUser.getEmail() != null && !adminUser.getEmail().trim().isEmpty()) {
                    sendAnswerNotificationToAdmin(adminUser, answer);
                }
            }
        } catch (Exception e) {
            System.err.println("ADMIN에게 답변 알림 이메일 발송 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 ADMIN 사용자에게 답변 알림 이메일 발송
     */
    private void sendAnswerNotificationToAdmin(User adminUser, Answer answer) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminUser.getEmail());
            message.setSubject("[문의 시스템] 문의에 추가 답변이 등록되었습니다");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = answer.getRepliedAt().format(formatter);

            String content = String.format(
                    "[디딤솔루션 문의 답변]\n\n" +
                    "안녕하세요, %s님\n\n" +
                    "문의에 추가 답변이 등록되었습니다.\n\n" +
                    "문의 정보:\n" +
                    "- 티켓 번호:%s\n" +
                    "- 문의 제목: %s\n" +
                    "- 문의 작성자: %s\n" +
                    "- 답변 내용: %s\n" +
                    "- 답변 일시: %s\n\n" +
                    "문의내역에서 확인하실 수 있습니다.\n" +
                    "감사합니다.",
                    adminUser.getName(),
                    answer.getInquiry().getTickNumber(),
                    answer.getInquiry().getTitle(),
                    answer.getInquiry().getManager() != null ? answer.getInquiry().getManager().getName() : answer.getUser().getName(),
                    answer.getContent() != null ? answer.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            message.setFrom(mailFrom);
            mailSender.send(message);

            System.out.println("ADMIN에게 답변 알림 이메일 발송 완료: " + adminUser.getEmail());
        } catch (Exception e) {
            System.err.println("ADMIN에게 답변 알림 이메일 발송 실패 (" + adminUser.getEmail() + "): " + e.getMessage());
        }
    }

    /**
     * Synology Chat으로 답변 알림 전송
     */
    private void sendAnswerNotificationToSynologyChat(Answer answer) {
        try {
            String answerWriterRole = answer.getUser().getRole();
            
            // ADMIN이 답변한 경우에는 Synology Chat 알림을 보내지 않음
            if ("ADMIN".equals(answerWriterRole)) {
                System.out.println("ADMIN이 답변한 경우 Synology Chat 알림을 보내지 않습니다.");
                return;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = answer.getRepliedAt().format(formatter);

            String message = String.format(
                    "[디딤솔루션 문의 추가 답변]\n\n" +
                    "문의에 답변이 등록되었습니다.\n\n" +
                    "문의 정보:\n" +
                    "- 티켓 번호:%s\n" +
                    "- 문의 제목: %s\n" +
                    "- 답변 작성자: %s\n" +
                    "- 답변 내용: %s\n" +
                    "- 답변 일시: %s\n\n" +
                    "문의내역에서 확인하실 수 있습니다.",
                    answer.getInquiry().getTickNumber(),
                    answer.getInquiry().getTitle(),
                    answer.getUser().getName(),
                    answer.getContent() != null ? answer.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            sendSynologyChatMessage(message);
        } catch (Exception e) {
            System.err.println("Synology Chat 답변 알림 전송 실패: " + e.getMessage());
        }
    }
}