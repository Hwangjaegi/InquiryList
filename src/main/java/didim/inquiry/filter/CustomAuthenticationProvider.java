package didim.inquiry.filter;

import didim.inquiry.auth.CustomUserDetails;
import didim.inquiry.service.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomAuthenticationProvider implements AuthenticationProvider {
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationProvider(CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomUsernamePasswordAuthenticationToken authToken = (CustomUsernamePasswordAuthenticationToken) authentication;

        String username = (String) authToken.getPrincipal();
        String password = (String) authToken.getCredentials();
        String customerCode = authToken.getCustomerCode();

        // 로그인 검증: 아이디로 유저 정보 가져오기
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!(userDetails instanceof CustomUserDetails)) {
            throw new BadCredentialsException("Invalid user details type");
        }

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        System.out.println("다운캐스팅: (customerCode: " + customUserDetails.getCustomerCode() + " / username: " + customUserDetails.getUsername() + " / password: " + customUserDetails.getPassword() + " ? : " + customUserDetails.getAuthorities());

        // 1. 비밀번호 검증
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // 2. customerCode 검증
        if (!checkCustomerCode(userDetails, customerCode)) {
            System.out.println("customerCode 검증 실패: 입력값=" + customerCode + ", DB값=" + customUserDetails.getCustomerCode());
            throw new BadCredentialsException("Invalid customer code");
        }

        // 모든 검증 통과 시 인증 객체 반환
        return new CustomUsernamePasswordAuthenticationToken(userDetails, password, customerCode, userDetails.getAuthorities());
    }

    private boolean checkCustomerCode(UserDetails userDetails, String customerCode) {
        if (!(userDetails instanceof CustomUserDetails)) {
            System.out.println("instanceof CustomUserDetails False");
            return false;
        }

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        String dbCustomerCode = customUserDetails.getCustomerCode();

        // customerCode가 null이거나 빈 문자열인 경우 처리
        if (customerCode == null || customerCode.trim().isEmpty() || dbCustomerCode == null || dbCustomerCode.trim().isEmpty()) {
            System.out.println("customerCode 또는 DB customerCode가 null 또는 빈 문자열: 입력값=" + customerCode + ", DB값=" + dbCustomerCode);
            return false;
        }

        boolean isValid = dbCustomerCode.equals(customerCode);
        System.out.println("customerCode 검증 결과: 입력값=" + customerCode + ", DB값=" + dbCustomerCode + ", 결과=" + isValid);
        return isValid;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}