package didim.inquiry.controller;

import didim.inquiry.controller.absClass.BaseController;
import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.AnswerService;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.UserService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class AnswerController extends BaseController {

    private final UserService userService;
    private final InquiryService inquiryService;
    private final AnswerService answerService;
    @Autowired
    private TemplateEngine templateEngine;

    public AnswerController(UserService userService, InquiryService inquiryService, AnswerService answerService) {
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.answerService = answerService;
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

    @PostMapping("/answerWriteAjax")
    @ResponseBody
    public java.util.Map<String, Object> answerWriteAjax(Answer answer) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            User findUser = getCurrentUser();
            answer.setUser(findUser);
            answerService.saveAnswer(answer);
            // 답글 저장 후, DB에서 다시 조회(연관객체 포함, fetch join)
            Answer saveAnswer = answerService.getAnswerWithInquiryAndWriterById(answer.getId()).orElse(answer);

            Long inquiryId = saveAnswer.getInquiry().getId();
            Inquiry inquiry = inquiryService.getInquiryById(inquiryId).orElseThrow(() -> new IllegalArgumentException("해당 문의 없음"));;
            inquiry.setStatus("답변완료");
            inquiryService.saveInquiry(inquiry);

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
            String answerHtml = templateEngine.process("inquiry/answerFragment", context);
            result.put("success", true);
            result.put("answerHtml", answerHtml);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
