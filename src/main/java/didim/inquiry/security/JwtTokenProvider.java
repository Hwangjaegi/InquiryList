package didim.inquiry.security;

import didim.inquiry.auth.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-here-make-it-long-enough-for-security}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간
    private long jwtExpirationMs;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // CustomUserDetails인 경우 고객코드도 함께 저장
        String usernameWithCustomerCode = userDetails.getUsername();
        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            usernameWithCustomerCode = customUserDetails.getUsername() + "|" + customUserDetails.getCustomerCode();
        }
        
        return generateTokenFromUsername(usernameWithCustomerCode);
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        // JWT 형식: header.payload.signature
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
        );
        
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ("{\"sub\":\"" + username + "\",\"iat\":" + now.getTime() + ",\"exp\":" + expiryDate.getTime() + "}")
            .getBytes(StandardCharsets.UTF_8)
        );
        
        // HMAC-SHA256 서명 생성
        String signature = createHmacSignature(header + "." + payload);
        
        return header + "." + payload + "." + signature;
    }

    public String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // 간단한 JSON 파싱 (실제로는 JSON 라이브러리 사용 권장)
            if (payload.contains("\"sub\":")) {
                String sub = payload.split("\"sub\":\"")[1].split("\"")[0];
                return sub;
            }
            return null;
        } catch (Exception e) {
            System.out.println("토큰에서 사용자명 추출 실패: " + e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            System.out.println("=== JWT 토큰 검증 시작 ===");
            System.out.println("검증할 토큰: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("토큰 형식 오류");
                return false;
            }
            
            // HMAC-SHA256 서명 검증
            String expectedSignature = createHmacSignature(parts[0] + "." + parts[1]);
            if (!parts[2].equals(expectedSignature)) {
                System.out.println("서명 검증 실패");
                return false;
            }
            
            // 만료 시간 검증
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            System.out.println("페이로드: " + payload);
            if (payload.contains("\"exp\":")) {
                String expStr = payload.split("\"exp\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                long exp = Long.parseLong(expStr);
                if (System.currentTimeMillis() > exp) {
                    System.out.println("토큰 만료");
                    return false;
                }
            }
            
            System.out.println("JWT 토큰 검증 성공");
            return true;
            
        } catch (Exception e) {
            System.out.println("JWT 토큰 검증 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // HMAC-SHA256 서명 생성 (올바른 방식)
    private String createHmacSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 서명 생성 실패", e);
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            if (payload.contains("\"exp\":")) {
                String expStr = payload.split("\"exp\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                long exp = Long.parseLong(expStr);
                return new Date(exp);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
} 