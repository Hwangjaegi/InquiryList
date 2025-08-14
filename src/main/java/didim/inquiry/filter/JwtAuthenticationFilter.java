package didim.inquiry.filter;

import didim.inquiry.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();
        
        // 정적 리소스는 필터 자체를 거치지 않도록 설정
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/image/") ||
               requestURI.startsWith("/temp/") ||
               requestURI.startsWith("/posts/") ||
               requestURI.startsWith("/uploads/") ||
               requestURI.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // shouldNotFilter에서 처리되므로 여기서는 정적 리소스 체크 불필요
        String requestURI = request.getRequestURI();
        
        System.out.println("=== JWT 필터 진입 ===");
        System.out.println("요청 URL: " + requestURI);
        
        // 공개 경로는 JWT 필터를 거치지 않음
        if (requestURI.equals("/login") ||
            requestURI.equals("/signup") ||
            requestURI.startsWith("/api/check-")) {
            
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
                System.out.println("SecurityContextHolder.getContext().getAuthentication() : " + SecurityContextHolder.getContext().getAuthentication());
                System.out.println("SecurityContextHolder.getContext().getAuthentication().isAuthenticated() : " + SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
                System.out.println("인증된 사용자 JWT 검증건너뛰기");
                filterChain.doFilter(request, response);
                return;
            }

            // 인증필요시 요청에서 jwt가져오기(헤더,쿠키,파라미터)
            //JWT → 유저 이름 추출 → DB에서 유저 정보 조회 → 인증 객체 생성 → SecurityContext에 저장
            // 로그인시 토큰이 없기에 NULL로 다음 필터
            String jwt = getJwtFromRequest(request);
            System.out.println("헤더 쿠키 파라미터 -> Filter JWT : " + jwt);

            if (StringUtils.hasText(jwt)) {
                System.out.println("jwt 가져오기 후 검증 시작");
                boolean isValid = jwtTokenProvider.validateToken(jwt);
                
                if (isValid) {
                    System.out.println("검증 성공 후 유저네임을 추출해 userDetailService 메서드호출");
                    String usernameWithCustomerCode = jwtTokenProvider.getUsernameFromToken(jwt);
                    
                    System.out.println("JWT 토큰에서 추출한 사용자명: " + usernameWithCustomerCode);
                    System.out.println("유저네임을 통해 DB에서 유저조회후 계정정보 및 권한 추출");
                    UserDetails userDetails = userDetailsService.loadUserByUsername(usernameWithCustomerCode);
                    
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
            System.out.println("헤더에서 토큰확인");
            return bearerToken.substring(7);
        }
        
        // 2. 쿠키에서 토큰 확인
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
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


        System.out.println("/login 요청 시 토큰 정보 확인 불가능");
        return null;
    }
} 