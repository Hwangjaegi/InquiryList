package didim.inquiry.service;

import didim.inquiry.domain.Customer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.domain.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    /**
     * 문의 작성 시 ADMIN 권한 사용자들에게 이메일 발송
     */
    public void sendInquiryNotification(Inquiry inquiry , String company) {
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
                    sendInquiryNotificationToAdmin(adminUser, inquiry , company);
                }
            }
        } catch (Exception e) {
            System.err.println("이메일 발송 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 ADMIN 사용자에게 문의 알림 이메일 발송
     */
    private void sendInquiryNotificationToAdmin(User adminUser, Inquiry inquiry , String company) {
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
                            "- 회사명: %s\n" +
                            "- 담당자: %s\n" +
                            "- 프로젝트: %s\n" +
                            "- 제목: %s\n" +
                            "- 내용: %s\n" +
                            "- 등록일시: %s\n\n" +
                            "관리자 콘솔에서 확인하실 수 있습니다.\n" +
                            "감사합니다.",
                    adminUser.getName(),
                    company,
                    inquiry.getManager() != null ? inquiry.getManager().getName() : inquiry.getWriter().getName(),
                    inquiry.getProject() != null ? inquiry.getProject().getSubject() : "기타문의",
                    inquiry.getTitle(),
                    inquiry.getContent() != null ? inquiry.getContent().replaceAll("<br>" , "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            mailSender.send(message);

            System.out.println("이메일 발송 완료: " + adminUser.getEmail());
        } catch (Exception e) {
            System.err.println("이메일 발송 실패 (" + adminUser.getEmail() + "): " + e.getMessage());
        }
    }


    /**
     * 답변 작성 시 알림 이메일 발송
     * ADMIN이 답변하면 문의 작성자에게, 문의 작성자가 답변하면 ADMIN에게 발송
     */
    public void sendAnswerNotification(Answer answer) {
        try {
            String answerWriterRole = answer.getUser().getRole();
            String answerWriterEmail = answer.getUser().getEmail();

            if ("ADMIN".equals(answerWriterRole)) {
                // ADMIN이 답변한 경우 -> 문의 작성자에게 알림
                sendAnswerNotificationToInquiryWriter(answer);
            } else {
                // 문의 작성자가 답변한 경우 -> ADMIN들에게 알림
                sendAnswerNotificationToAdmins(answer);
            }
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
                System.out.println("문의 작성자의 이메일이 없습니다.");
                return;
            }

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
                            "- 문의 제목: %s\n" +
                            "- 답변 작성자: %s\n" +
                            "- 답변 내용: %s\n" +
                            "- 답변 일시: %s\n\n" +
                            "문의 목록에서 확인하실 수 있습니다.\n" +
                            "감사합니다.",
                    inquiryWriter.getName(),
                    answer.getInquiry().getTitle(),
                    answer.getUser().getName(),
                    answer.getContent() != null ? answer.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            mailSender.send(message);

            System.out.println("문의 작성자에게 답변 알림 이메일 발송 완료: " + toEmail);
        } catch (Exception e) {
            System.err.println("문의 작성자에게 답변 알림 이메일 발송 실패: " + e.getMessage());
        }
    }

    /**
     * 문의 작성자가 답변했을 때 ADMIN들에게 알림 발송
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

            // 각 ADMIN 사용자에게 이메일 발송
            for (User adminUser : adminUsers) {
                if (adminUser.getEmail() != null && !adminUser.getEmail().trim().isEmpty()) {
                    sendAnswerNotificationToAdmin(adminUser, answer);
                }
            }
        } catch (Exception e) {
            System.err.println("ADMIN들에게 답변 알림 이메일 발송 중 오류 발생: " + e.getMessage());
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
                            "- 문의 제목: %s\n" +
                            "- 문의 작성자: %s\n" +
                            "- 답변 내용: %s\n" +
                            "- 답변 일시: %s\n\n" +
                            "관리자 콘솔에서 확인하실 수 있습니다.\n" +
                            "감사합니다.",
                    adminUser.getName(),
                    answer.getInquiry().getTitle(),
                    answer.getInquiry().getManager() != null ? answer.getInquiry().getManager().getName() : answer.getUser().getName(),
                    answer.getContent() != null ? answer.getContent().replaceAll("<br>", "\n").replaceAll("<[^>]*>", "") : "",
                    formattedDate
            );

            message.setText(content);
            mailSender.send(message);

            System.out.println("ADMIN에게 답변 알림 이메일 발송 완료: " + adminUser.getEmail());
        } catch (Exception e) {
            System.err.println("ADMIN에게 답변 알림 이메일 발송 실패 (" + adminUser.getEmail() + "): " + e.getMessage());
        }
    }
}