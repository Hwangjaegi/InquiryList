package didim.inquiry.service;

import didim.inquiry.auth.CustomUserDetails;
import didim.inquiry.domain.User;
import didim.inquiry.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // 생성자 주입
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameWithCustomerCode) throws UsernameNotFoundException {
        String[] parts = usernameWithCustomerCode.split("\\|");

        if (parts.length != 2) {
            throw new UsernameNotFoundException("아이디 또는 고객코드 형식이 올바르지 않습니다.");
        }

        String username = parts[0];
        String customerCode = parts[1];

        System.out.println("username : " + username + " / customerCode : " + customerCode);

        User user = userRepository.findByUsernameAndCustomerCode(username,customerCode)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다: " + username));

        if (user.getDeleteFlag()) {
            throw new UsernameNotFoundException("비활성화된 계정입니다");
        }

        System.out.println("LoadUserByUsername = username : " + user.getUsername() + " password : " + user.getPassword() + " customerCode : " + user.getCustomerCode());

        // 기존 UserDetails 대신 CustomUserDetails 객체 반환
        return new CustomUserDetails(user);
    }
}
