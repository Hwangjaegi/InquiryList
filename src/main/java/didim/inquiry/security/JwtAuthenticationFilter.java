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
        
        // JWT 필터는 인증이 필요한 요청에만 적용
        String requestURI = request.getRequestURI();
        
        System.out.println("=== JWT 필터 진입 ===");
        System.out.println("요청 URL: " + requestURI);
        
        // 공개 경로는 JWT 필터를 거치지 않음 (SecurityConfig에서 이미 permitAll 처리됨)
        if (requestURI.equals("/login") ||
            requestURI.equals("/signup") ||
            requestURI.startsWith("/api/auth/") ||
            requestURI.startsWith("/api/check-") ||
            requestURI.startsWith("/css/") ||
            requestURI.startsWith("/js/") ||
            requestURI.startsWith("/image/") ||
            requestURI.startsWith("/temp/") ||
            requestURI.startsWith("/posts/") ||
            requestURI.startsWith("/uploads/") ||
            requestURI.startsWith("/error")) {
            
            System.out.println("공개 경로 - JWT 검증 건너뛰기: " + requestURI);
            // 공개 경로는 JWT 검증 없이 통과
            filterChain.doFilter(request, response);
            return;
        }
        
        // 인증이 필요한 경로만 JWT 검증 수행
        System.out.println("인증 필요 경로 - JWT 검증 수행: " + requestURI);
        
        try {
            // 이미 인증된 사용자가 있으면 JWT 검증 건너뛰기
            if (SecurityContextHolder.getContext().getAuthentication() != null && 
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                System.out.println("JWT 검증건너뛰기");
                filterChain.doFilter(request, response);
                return;
            }
            
            String jwt = getJwtFromRequest(request);
            System.out.println("Filter JWT : " + jwt);

            if (StringUtils.hasText(jwt)) {
                boolean isValid = jwtTokenProvider.validateToken(jwt);
                
                if (isValid) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("JWT 필터 오류: " + ex.getMessage(), ex);
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
                    System.out.println("JWT 필터에서 쿠키에서 토큰 발견");
                    return cookie.getValue();
                }
            }
        }
        
        // 3. URL 파라미터에서 토큰 확인 (추가)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            System.out.println("URL 파라미터에서 토큰 발견: " + tokenParam.substring(0, Math.min(50, tokenParam.length())) + "...");
            return tokenParam;
        }

        System.out.println("토큰 정보 확인 불가능");
        return null;
    }
} 