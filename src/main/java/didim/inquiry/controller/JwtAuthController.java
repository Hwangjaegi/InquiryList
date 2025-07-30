package didim.inquiry.controller;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import didim.inquiry.dto.SearchInquiryDto;
import didim.inquiry.security.JwtTokenProvider;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/api/auth")
public class JwtAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final InquiryService inquiryService;

    public JwtAuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService, InquiryService inquiryService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.inquiryService = inquiryService;
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            System.out.println("=== JWT 로그인 시도 ===");
            System.out.println("사용자명: " + loginRequest.getUsername());
            System.out.println("비밀번호: " + (loginRequest.getPassword() != null ? "***" : "null"));
            
            // 인증 처리
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            System.out.println("인증 성공: " + authentication.getName());
            System.out.println("인증 권한: " + authentication.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // JWT 토큰 생성
            String jwt = jwtTokenProvider.generateToken(authentication);
            System.out.println("JWT 토큰 생성 완료: " + jwt.substring(0, Math.min(50, jwt.length())) + "...");

            // 토큰이 실제로 발행되었는지 확인
            boolean tokenExists = jwtTokenProvider.validateToken(jwt);
            System.out.println("토큰 발행 확인 결과: " + tokenExists);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwt);
            response.put("message", "로그인 성공");
            response.put("redirectUrl", "/api/auth/inquiryList");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("=== JWT 로그인 실패 ===");
            System.out.println("예외 타입: " + e.getClass().getSimpleName());
            System.out.println("예외 메시지: " + e.getMessage());
            System.err.println("존재하지 않는 계정.");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "로그인 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/test")
    public String testPage() {
        return "jwt-test";
    }

    @GetMapping("/validate")
    @ResponseBody
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (jwtTokenProvider.validateToken(jwt)) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", username);
                    return ResponseEntity.ok(response);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "유효하지 않은 토큰");
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "토큰 검증 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/inquiryList")
    public String inquiryListByApi(Model model, HttpServletRequest request,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(value = "token", required = false) String token) {

        System.out.println("=== 컨트롤러에서 직접 토큰 처리 ===");
        System.out.println("요청 시간: " + new Date());
        System.out.println("요청 URL: " + request.getRequestURL());
        System.out.println("URL 파라미터 토큰: " + (token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null"));

        try {
            // 1. URL 파라미터에서 토큰 확인 , api 로그인 후 새로고침 시 토큰이 사라지는 문제가있어 쿠키에서 조회하는 방식으로 설정
            if (token == null) {
                System.out.println("URL 파라미터에 토큰이 없음, 쿠키에서 토큰 확인");
                
                // 쿠키에서 JWT 토큰 확인
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("jwt_token".equals(cookie.getName())) {
                            token = cookie.getValue();
                            System.out.println("쿠키에서 JWT 토큰 발견: " + token.substring(0, Math.min(50, token.length())) + "...");
                            break;
                        }
                    }
                }
                
                // 쿠키에도 토큰이 없으면 세션 인증 확인
                if (token == null) {
                    System.out.println("쿠키에도 토큰이 없음, 세션 인증 확인");
                    
                    // 세션 인증 상태 확인
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.isAuthenticated() &&
                        !"anonymousUser".equals(authentication.getName())) {

                        System.out.println("세션 인증 유효함: " + authentication.getName());

                        // 세션 인증으로 사용자 정보 조회
                        String username = authentication.getName();
                        User findUser = userService.getUserByUsername(username);
                        if (findUser == null) {
                            System.out.println("세션 사용자를 찾을 수 없음: " + username);
                            return "redirect:/login?error=user_not_found";
                        }

                        String role = findUser.getRole();
                        System.out.println("세션 사용자 역할: " + role);

                        // 세션 인증으로 inquiryList 처리
                        return processInquiryList(model, role, username, page);
                    } else {
                        System.out.println("세션 인증도 유효하지 않음");
                        return "redirect:/login?error=no_token";
                    }
                }
            }

            // 2. JWT 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                System.out.println("JWT 토큰이 유효하지 않음");
                return "redirect:/login?error=invalid_token";
            }

            // 3. 토큰에서 사용자명 추출
            String usernameWithCustomerCode = jwtTokenProvider.getUsernameFromToken(token);
            System.out.println("토큰에서 추출한 사용자명: " + usernameWithCustomerCode);

            if (usernameWithCustomerCode == null) {
                System.out.println("토큰에서 사용자명을 추출할 수 없음");
                return "redirect:/login?error=token_parse_failed";
            }

            // 4. 사용자명 파싱 (username|customerCode 형식)
            String username;
            if (usernameWithCustomerCode.contains("|")) {
                username = usernameWithCustomerCode.split("\\|")[0];
            } else {
                username = usernameWithCustomerCode;
            }
            System.out.println("파싱된 사용자명: " + username);

            // 5. 사용자 정보 조회
            User findUser = userService.getUserByUsername(username);
            if (findUser == null) {
                System.out.println("사용자를 찾을 수 없음: " + username);
                return "redirect:/login?error=user_not_found";
            }

            String role = findUser.getRole();
            System.out.println("사용자 역할: " + role);

            // 6. JWT 토큰으로 inquiryList 처리
            return processInquiryList(model, role, username, page);

        } catch (Exception e) {
            System.out.println("=== 컨트롤러 토큰 처리 중 오류 발생 ===");
            System.out.println("예외 타입: " + e.getClass().getSimpleName());
            System.out.println("예외 메시지: " + e.getMessage());
            e.printStackTrace();
            
            return "redirect:/login?error=token_processing_failed";
        }
    }
    
    // 공통 inquiryList 처리 메서드
    private String processInquiryList(Model model, String role, String username, int page) {
        try {
            // 검색 조건 설정
            SearchInquiryDto searchInquiryDto = new SearchInquiryDto();
            if (role.equals("ADMIN")) {
                searchInquiryDto.setStatus(Arrays.asList("답변 대기중", "답변완료"));
            }

            // 페이지당 10개 문의 조회 (페이징 적용)
            Pageable pageable = PageRequest.of(page, 10);
            Page<Inquiry> inquiries = inquiryService.getInquiryBySearch(searchInquiryDto, role, username, pageable);

            // 문의 내용 줄바꿈 기호 <br>로 변경
            inquiries.forEach(inquiry -> {
                String content = inquiry.getContent();
                if (content != null) {
                    inquiry.setContent(content.replaceAll("\n", "<br>"));
                }

                if (inquiry.getAnswers() != null) {
                    inquiry.getAnswers().forEach(answer -> {
                        String answerContent = answer.getContent();
                        if (answerContent != null) {
                            answer.setContent(answerContent.replaceAll("\n", "<br>"));
                        }
                    });

                    // 답글을 작성일 순서로 정렬
                    inquiry.getAnswers().sort(Comparator.comparing(Answer::getRepliedAt));
                }
            });

            System.out.println("조회된 문의 개수: " + inquiries.getTotalElements());
            System.out.println("현재 페이지 문의 개수: " + inquiries.getContent().size());

            // 모델에 데이터 추가
            model.addAttribute("answerCount", 1);
            model.addAttribute("inquiries", inquiries);
            model.addAttribute("searchInquiry", searchInquiryDto);
            model.addAttribute("role", role);
            
            User findUser = userService.getUserByUsername(username);
            model.addAttribute("user", findUser);

            System.out.println("세션 인증으로 inquiryList 페이지 렌더링 완료");
            return "inquiry/inquiryList";
            
        } catch (Exception e) {
            System.out.println("=== 세션 인증 inquiryList 처리 중 오류 발생 ===");
            System.out.println("예외 타입: " + e.getClass().getSimpleName());
            System.out.println("예외 메시지: " + e.getMessage());
            e.printStackTrace();
            
            return "redirect:/login?error=processing_failed";
        }
    }

    // 로그인 요청 DTO
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
} 