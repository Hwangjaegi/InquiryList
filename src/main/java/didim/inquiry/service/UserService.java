package didim.inquiry.service;

import didim.inquiry.domain.User;
import didim.inquiry.repository.AdminRepository;
import didim.inquiry.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    //패스워드 암호화 인터페이스 (시큐리티에 포함되어있음)
    private final PasswordEncoder passwordEncoder;



    public UserService(UserRepository userRepository, AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //1. 아이디가 존재하는지 확인 후 존재하지 않으면 가입 처리
    public boolean signUpUser(User user){
        // 고객코드가 생성되어있고 활성화 되어있는지 확인 있으면 true
        boolean exists = adminRepository.existsByCodeAndStatus(user.getCustomerCode(),"ACTIVE");
        if(!exists){
            return false;
        }

        // 고객코드가 같은 유저에서 아이디가 같은경우 true
        if (userRepository.findByUsernameAndCustomerCode(user.getUsername() , user.getCustomerCode()).isPresent()){ //isPresent : 객체의 값이 존재하는지 확인
            return false;
        }

        // 고객코드로 처음 가입한 사람은 관리자 권한 부여


        //가입된 정보가 없을 경우 패스워드 암호화 처리 후 가입처리
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return true;
    }

    //2. id를 통해 회원정보 조회
    public Optional<User> getUserById(Long id){
        return userRepository.findById(id);
    }

    // 3. 계정 아이디(username)를 통해 회원정보 조회
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("로그인한 계정 정보가 존재하지 않습니다."));
    }

    public Page<User> getUsersByRole(String role , Pageable pageable) {
        return userRepository.findAllByRoleOrderByIdDesc(role , pageable);
    }

    //계정아이디 또는 이메일로 검색도 가능한 메서드명
    public Page<User> searchUsersByRoleAndKeyword(String role, String keyword, Pageable pageable) {
        return userRepository.findAllByRoleAndUsernameContainingOrEmailContainingOrderByIdDesc(
                role, keyword, keyword , pageable);
    }

    //실제론 삭제하지 않고 DeleteFlag를 true로 바꾼다.
    @Transactional
    public void deleteUserById(long id) {
        userRepository.UserDeleteFlagTrueById(id);
    }


    @Transactional
    public void restoreUserById(Long id) {
        userRepository.UserDeleteFlagFalseById(id);
    }

    public long getUsersCount() {
        return userRepository.count();
    }
}
