package didim.inquiry.security;

import didim.inquiry.auth.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-here-make-it-long-enough-for-security}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간
    private long jwtExpirationMs;

    // 임시 토큰 저장소 (실제로는 Redis나 DB 사용 권장)
    private Map<String, TokenInfo> tokenStore = new HashMap<>();

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // CustomUserDetails인 경우 고객코드도 함께 저장
        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            String usernameWithCustomerCode = customUserDetails.getUsername() + "|" + customUserDetails.getCustomerCode();
            return generateTokenFromUsername(usernameWithCustomerCode);
        }
        
        return generateTokenFromUsername(userDetails.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        // 간단한 토큰 생성 (실제로는 JWT 라이브러리 사용)
        String token = "JWT_" + username + "_" + now.getTime() + "_" + jwtSecret.hashCode();
        
        // 토큰 정보 저장
        TokenInfo tokenInfo = new TokenInfo(username, expiryDate);
        tokenStore.put(token, tokenInfo);
        
        return token;
    }

    public String getUsernameFromToken(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo != null && !tokenInfo.isExpired()) {
            return tokenInfo.getUsername();
        }
        return null;
    }

    public boolean validateToken(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo != null && !tokenInfo.isExpired();
    }

    public Date getExpirationDateFromToken(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo != null ? tokenInfo.getExpiryDate() : null;
    }

    public boolean isTokenExpired(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo == null || tokenInfo.isExpired();
    }
    
    // 토큰 정보를 저장하는 내부 클래스
    private static class TokenInfo {
        private final String username;
        private final Date expiryDate;
        
        public TokenInfo(String username, Date expiryDate) {
            this.username = username;
            this.expiryDate = expiryDate;
        }
        
        public String getUsername() {
            return username;
        }
        
        public Date getExpiryDate() {
            return expiryDate;
        }
        
        public boolean isExpired() {
            return new Date().after(expiryDate);
        }
    }
} 