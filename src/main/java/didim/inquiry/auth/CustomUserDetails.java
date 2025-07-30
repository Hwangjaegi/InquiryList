package didim.inquiry.auth;

import didim.inquiry.domain.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // 필요에 따라 메서드 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getPassword() {
        System.out.println("=== CustomUserDetails.getPassword 호출 ===");
        System.out.println("반환할 비밀번호: " + (user.getPassword() != null ? "***" : "null"));
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        System.out.println("=== CustomUserDetails.getUsername 호출 ===");
        System.out.println("반환할 사용자명: " + user.getUsername());
        return user.getUsername(); // customerCode는 여기에 포함할 필요 없음
    }
    
    public String getCustomerCode() {
        return user.getCustomerCode();
    }
}
