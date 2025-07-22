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
    private String username;
    private String password;
    private String customerCode;
    private List<GrantedAuthority> authorities;

    public CustomUserDetails(User user){
        System.out.println("CustomUserDetails (username : " + user.getUsername() + " / password : " + user.getPassword() + " / customerCode : " + user.getCustomerCode() );
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.customerCode = user.getCustomerCode();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
