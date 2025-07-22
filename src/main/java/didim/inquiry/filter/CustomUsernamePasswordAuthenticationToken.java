package didim.inquiry.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
     private final String customerCode;

    public CustomUsernamePasswordAuthenticationToken(Object principal , Object credentials , String customerCode) {
        super(principal , credentials);
        this.customerCode = customerCode;
    }

    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials, String customerCode, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.customerCode = customerCode;
    }

    public String getCustomerCode(){
        return customerCode;
    }
}
