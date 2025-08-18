package didim.inquiry.service;

import didim.inquiry.auth.CustomUserDetails;
import didim.inquiry.domain.Customer;
import didim.inquiry.domain.User;
import didim.inquiry.repository.CustomerRepository;
import didim.inquiry.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    // 생성자 주입
    public CustomUserDetailsService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameWithCustomerCode) throws UsernameNotFoundException {
        String[] parts = usernameWithCustomerCode.split("\\|");

        if (parts.length != 2) {
            System.err.println("사용자명 형식 오류: " + usernameWithCustomerCode);
            throw new UsernameNotFoundException("아이디 또는 고객코드 형식이 올바르지 않습니다.");
        }

        String username = parts[0];
        String customerCode = parts[1];

        //customercode 삭제 , 비활성화 여부 확인
        Customer customer = customerRepository.findByCode(customerCode);
        if (customer == null){
            System.err.println("존재하지 않거나 비활성화된 고객코드 입니다.");
            throw new UsernameNotFoundException("존재하지 않거나 비활성화된 고객코드 입니다.");
        }else if(!customer.getStatus().equals("ACTIVE")){
            System.err.println("비활성화된 고객코드 입니다.");
            throw new UsernameNotFoundException("비활성화된 고객코드 입니다.");
        }

        User user = userRepository.findByUsernameAndCustomerCode(username,customerCode)
                .orElseThrow(() -> {
                    System.err.println("사용자를 찾을 수 없음: " + username);
                    return new UsernameNotFoundException("존재하지 않는 계정입니다: " + username);
                });

        if (user.getDeleteFlag()) {
            System.err.println("비활성화된 계정: " + username);
            throw new UsernameNotFoundException("비활성화된 계정입니다");
        }

        // 기존 UserDetails 대신 CustomUserDetails 객체 반환
        return new CustomUserDetails(user);
    }
}
