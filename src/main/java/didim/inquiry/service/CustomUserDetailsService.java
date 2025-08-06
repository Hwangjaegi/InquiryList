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
        System.out.println("=== CustomUserDetailsService.loadUserByUsername 호출 ===");
        System.out.println("2. 입력된 usernameWithCustomerCode: " + usernameWithCustomerCode);
        
        String[] parts = usernameWithCustomerCode.split("\\|");

        if (parts.length != 2) {
            System.err.println("사용자명 형식 오류: " + usernameWithCustomerCode);
            throw new UsernameNotFoundException("아이디 또는 고객코드 형식이 올바르지 않습니다.");
        }

        String username = parts[0];
        String customerCode = parts[1];

        System.out.println("파싱된 username: " + username);
        System.out.println("파싱된 customerCode: " + customerCode);

        //customercode 삭제 , 비활성화 여부 확인
        Customer customer = customerRepository.findByCode(customerCode);
        if (customer == null){
            System.err.println("존재하지 않거나 비활성화된 고객코드 입니다.");
            throw new UsernameNotFoundException("존재하지 않거나 비활성화된 고객코드 입니다.");
        }else if(!customer.getStatus().equals("ACTIVE")){
            System.err.println("비활성화된 고객코드 입니다.");
            throw new UsernameNotFoundException("비활성화된 고객코드 입니다.");
        }


        System.out.println("파싱된 username , customerCode로 DB조회");
        User user = userRepository.findByUsernameAndCustomerCode(username,customerCode)
                .orElseThrow(() -> {
                    System.out.println("사용자를 찾을 수 없음: " + username);
                    return new UsernameNotFoundException("존재하지 않는 계정입니다: " + username);
                });

        System.out.println("DB에서 조회된 사용자: " + user.getUsername());
        System.out.println("사용자 고객코드: " + user.getCustomerCode());
        System.out.println("사용자 삭제플래그: " + user.getDeleteFlag());

        if (user.getDeleteFlag()) {
            System.out.println("비활성화된 계정: " + username);
            throw new UsernameNotFoundException("비활성화된 계정입니다");
        }

        System.out.println("인증 성공 - CustomUserDetails 생성(User객체) 후 반환 -> JwtAuthController");
        // 기존 UserDetails 대신 CustomUserDetails 객체 반환
        return new CustomUserDetails(user);
    }
}
