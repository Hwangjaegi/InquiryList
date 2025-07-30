package didim.inquiry.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 특정 URL은 JWT 필터를 거치지 않음
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/auth/inquiryList")) {
            System.out.println("=== JWT 필터 제외 ===");
            System.out.println("요청 URL: " + requestURI + " - JWT 필터를 거치지 않음");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            System.out.println("=== JWT 필터 실행 ===");
            System.out.println("요청 URL: " + request.getRequestURI());
            System.out.println("JWT 토큰: " + (jwt != null ? jwt.substring(0, Math.min(50, jwt.length())) + "..." : "null"));

            if (StringUtils.hasText(jwt)) {
                System.out.println("토큰이 존재함, 검증 시작");
                boolean isValid = jwtTokenProvider.validateToken(jwt);
                System.out.println("토큰 검증 결과: " + isValid);
                
                if (isValid) {
                    System.out.println("JWT 토큰 유효함");
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    System.out.println("토큰에서 추출한 사용자명: " + username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("UserDetails 로드 완료: " + userDetails.getUsername());
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("SecurityContext에 인증 정보 설정 완료");
                } else {
                    System.out.println("JWT 토큰이 유효하지 않음");
                }
            } else {
                System.out.println("JWT 토큰이 없음");
            }
        } catch (Exception ex) {
            System.out.println("JWT 필터 오류: " + ex.getMessage());
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. 쿠키에서 토큰 확인
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

//        // 3. URL 파라미터에서 토큰 확인 (추가)
//        String tokenParam = request.getParameter("token");
//        if (StringUtils.hasText(tokenParam)) {
//            System.out.println("URL 파라미터에서 토큰 발견: " + tokenParam.substring(0, Math.min(50, tokenParam.length())) + "...");
//            return tokenParam;
//        }
        
        return null;
    }
} 