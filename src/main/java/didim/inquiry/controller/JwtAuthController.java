package didim.inquiry.controller;

import didim.inquiry.domain.Answer;
import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.Project;
import didim.inquiry.domain.User;
import didim.inquiry.dto.SearchInquiryDto;
import didim.inquiry.security.JwtTokenProvider;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.ProjectService;
import didim.inquiry.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/api/auth")
public class JwtAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final InquiryService inquiryService;
    private final ProjectService projectService;

    public JwtAuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService, InquiryService inquiryService, ProjectService projectService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.projectService = projectService;
    }

    // 관리자용 최소 정보 회원가입 (이름, 이메일, 전화번호 공란 허용)
    @PostMapping("/minimal-signup")
    public String minimalSignup(@RequestParam String customerCode,
                               @RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String tel,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = new User();
        user.setCustomerCode(customerCode);
        user.setUsername(username);
        user.setPassword(password);
        user.setName(name == null ? "" : name);
        user.setEmail(email == null ? "" : email);
        user.setTel(tel == null ? "" : tel);
        // 기본 권한 USER로 설정 (필요시 로직 조정)
        user.setRole("USER");
        boolean success = userService.signUpUserAllowBlank(user);
        if (!success) {
            model.addAttribute("errorMessage", "가입에 실패했습니다. (중복/고객코드/비밀번호 등 확인)");
            return "login";
        }
        redirectAttributes.addFlashAttribute("successMessage", "계정이 생성되었습니다. 사용자에게 계정정보를 전달하세요.");
        return "redirect:/admin/customerList";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request ,
                                   @RequestParam(value = "customerId" , required = false) String customerId  ) {
        try {
            System.out.println("=== JwtAuthController JWT 로그인 시도 ===");
            System.out.println("사용자명: " + loginRequest.getUsername());
            System.out.println("비밀번호: " + (loginRequest.getPassword() != null ? "***" : "null"));
            
            // 인증 처리
            System.out.println("1. 로그인 정보 인증처리 -> CustomUserDetailsService");
            //내부적으로 userDetailService를 사용해 사용자정보조회 및 검증 실행
            //따라서 해당 클래스를 구현한 CustomUserDetailsService 클래스의 메서드 실행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            System.out.println("인증성공 시 username,password만 가지고 인증객체 생성");
            System.out.println("인증 성공: " + authentication.getName());
            System.out.println("인증 권한: " + authentication.getAuthorities());

//            System.out.println("인증객체를 시큐리티 컨텍스트에 저장");
//            SecurityContextHolder.getContext().setAuthentication(authentication);

            // JWT 토큰 생성
            System.out.println("인증객체를 JWT토큰으로 생성 (header.payload,signature)");
            String jwt = jwtTokenProvider.generateToken(authentication);
            System.out.println("JWT 토큰 생성 완료: " + jwt.substring(0, Math.min(50, jwt.length())) + "...");

            // 토큰이 실제로 발행되었는지 확인
            boolean tokenExists = jwtTokenProvider.validateToken(jwt);
            System.out.println("토큰 발행 검증 확인 결과: " + tokenExists);

            // 토큰 발행 후 응답 결과 리턴
            System.out.println("요청한 클라이언트에게 토큰,url응답");
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

    @GetMapping("/clear-session")
    @ResponseBody
    public ResponseEntity<?> clearSession(HttpServletRequest request) {
        try {
            request.getSession().invalidate();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "세션이 무효화되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "세션 무효화 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("=== JWT 로그아웃 처리 ===");
            
            // 1. 세션 무효화
            request.getSession().invalidate();
            System.out.println("세션 무효화 완료");
            
            // 2. SecurityContext 클리어
            SecurityContextHolder.clearContext();
            System.out.println("SecurityContext 클리어 완료");
            
            // 3. JWT 토큰 쿠키 삭제
            jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt_token", "");
            jwtCookie.setMaxAge(0);
            jwtCookie.setPath("/");
            response.addCookie(jwtCookie);
            System.out.println("JWT 토큰 쿠키 삭제 완료");
            
            // 4. JSESSIONID 쿠키 삭제
            jakarta.servlet.http.Cookie sessionCookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
            sessionCookie.setMaxAge(0);
            sessionCookie.setPath("/");
            response.addCookie(sessionCookie);
            System.out.println("JSESSIONID 쿠키 삭제 완료");
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("message", "로그아웃이 완료되었습니다.");
            responseMap.put("redirectUrl", "/login");
            
            return ResponseEntity.ok(responseMap);
            
        } catch (Exception e) {
            System.out.println("로그아웃 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", false);
            responseMap.put("message", "로그아웃 처리 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(responseMap);
        }
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
                          @RequestParam(value = "token", required = false) String token,
                          @RequestParam(value = "projectId" , required = false) Long projectId  ){

        System.out.println("=== 컨트롤러에서 직접 토큰 처리 ===");
        System.out.println("요청 시간: " + new Date());
        System.out.println("요청 URL: " + request.getRequestURL());
        System.out.println("URL 파라미터 토큰: " + (token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null"));


        System.out.println("프로젝트 아이디 : " + projectId);
        if (projectId != null){
            HttpSession session = request.getSession();
            session.setAttribute("projectId",projectId);

            System.out.println("세션 프로젝트 아이디 : " + session.getAttribute("projectId"));
        }




        try {
            // 1. URL 파라미터에서 토큰 확인 , api 로그인 후 새로고침 시 토큰이 사라지는 문제가있어 쿠키에서 조회하는 방식으로 설정
            if (token == null) {
                System.out.println("URL 파라미터에 토큰이 없음 , 세션확인");


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
                        return processInquiryList(model, role, username, page, request);
                    } else {
                        System.out.println("세션 인증도 유효하지 않음");
                        return "redirect:/login?error=no_token";
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
            return processInquiryList(model, role, username, page, request);

        } catch (Exception e) {
            System.out.println("=== 컨트롤러 토큰 처리 중 오류 발생 ===");
            System.out.println("예외 타입: " + e.getClass().getSimpleName());
            System.out.println("예외 메시지: " + e.getMessage());
            e.printStackTrace();
            
            return "redirect:/login?error=token_processing_failed";
        }
    }
    
    // 공통 inquiryList 처리 메서드
    private String processInquiryList(Model model, String role, String username, int page, HttpServletRequest request) {
        try {
            // 검색 조건 설정
            SearchInquiryDto searchInquiryDto = new SearchInquiryDto();
            if (role.equals("ADMIN")) {
                searchInquiryDto.setStatus(Arrays.asList("답변 대기중", "답변완료"));
            }

            // 세션에서 projectId 가져오기
            HttpSession session = request.getSession();
            Long projectId = (Long) session.getAttribute("projectId");
            System.out.println("JWT 세션에서 가져온 projectId: " + projectId);
            
            // projectId가 있으면 검색 조건에 추가
            if (projectId != null) {
                searchInquiryDto.setProjectId(projectId);
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
            
            // 사용자의 고객코드에 해당하는 프로젝트 목록 추가
            String customerCode = findUser.getCustomerCode();
            List<Project> userProjects = (customerCode != null && !customerCode.isBlank()) ?
                    projectService.getProjectListByCustomerCode(customerCode, Pageable.unpaged()).getContent() :
                    List.of();
            model.addAttribute("projectList", userProjects);

            //FM api 로그인 시 프로젝트

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