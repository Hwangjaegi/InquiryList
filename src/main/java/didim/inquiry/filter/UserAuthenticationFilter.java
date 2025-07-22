package didim.inquiry.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;

public class UserAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        try {
            String username = obtainUsername(request);
            String password = obtainPassword(request);
            String customerCode = request.getParameter("customerCode");

            System.out.println("attemptAuthentication 시작 - username: " + username + ", customerCode: " + customerCode);

            username = username != null ? username.trim() : "";

            if (password == null) {
                password = "";
            }

            CustomUsernamePasswordAuthenticationToken authRequest = new CustomUsernamePasswordAuthenticationToken(username, password, customerCode);
            System.out.println("authRequest - username: " + authRequest.getName()); // UsernamePasswordAuthenticationToken은 getName()으로 username 제공
            System.out.println("authRequest - password: " + authRequest.getCredentials()); // getCredentials()는 password
            System.out.println("authRequest - customerCode: " + authRequest.getCustomerCode()); // 커스텀 필드 직접 getter 필요
            setDetails(request, authRequest);

            System.out.println("this 시작");
            Authentication authentication = this.getAuthenticationManager().authenticate(authRequest);
            System.out.println("this 끝");

            System.out.println("authenticate 호출 후 성공 - authentication: " + authentication);

            return authentication;
        } catch (Exception e) {
            System.out.println("attemptAuthentication 에서 예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;  // 예외를 던져야 실패 핸들러가 작동합니다.
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        System.out.println("successfulAuthentication 호출됨 - 인증 성공");

        SecurityContextHolder.getContext().setAuthentication(authResult);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        response.sendRedirect("/inquiryList");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              org.springframework.security.core.AuthenticationException failed)
            throws IOException, ServletException {
        System.out.println("unsuccessfulAuthentication 호출됨 - 인증 실패: " + failed.getMessage());

        super.unsuccessfulAuthentication(request, response, failed);  // 기본 실패 처리 호출

        // 필요하면 직접 실패 시 리다이렉트도 할 수 있습니다:
        // response.sendRedirect("/login?error=true");
    }


}
