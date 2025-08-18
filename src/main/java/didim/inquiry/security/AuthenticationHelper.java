package didim.inquiry.security;

import didim.inquiry.common.BaseController;
import didim.inquiry.service.UserService;
import org.springframework.stereotype.Component;

import didim.inquiry.domain.User;
import jakarta.servlet.http.HttpServletRequest;

// jwt 토큰추출 및 사용자 검증 , 없으면 세션 검증
@Component
public class AuthenticationHelper extends BaseController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public AuthenticationHelper(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    // JWT 토큰에서 사용자 정보 가져오기
    public User getCurrentUserFromToken(HttpServletRequest request) {
        //http 요청 파라미터에서 토큰 추출
        String token = extractTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String usernameWithCustomerCode = jwtTokenProvider.getUsernameFromToken(token);

            System.out.println("JWT 토큰에서 추출한 사용자명: " + usernameWithCustomerCode);

            // username|customerCode 형태인 경우
            if (usernameWithCustomerCode != null && usernameWithCustomerCode.contains("|")) {
                String[] parts = usernameWithCustomerCode.split("\\|");
                if (parts.length == 2) {
                    String username = parts[0];
                    String customerCode = parts[1];
                    return userService.getUserByUsernameAndCustomerCode(username, customerCode);
                }
            }

            // username만 있는 경우 (기존 방식)
            return userService.getUserByUsername(usernameWithCustomerCode);
        }
        return getCurrentUser(); // JWT 토큰이 없으면 세션 기반 인증 사용
    }

    // 요청에서 JWT 토큰 추출
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. URL 파라미터에서 토큰 확인
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam;
        }

        return null;
    }
}
