package didim.inquiry.service;

import didim.inquiry.domain.User;
import didim.inquiry.dto.UserDto;
import didim.inquiry.repository.AdminRepository;
import didim.inquiry.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        // user에서 아이디가 중복되면 가입 못하게 방지
        if (userRepository.findByUsername(user.getUsername()).isPresent()){ //isPresent : 객체의 값이 존재하는지 확인
            return false;
        }

        // 고객코드로 처음 가입한 사람은 관리자 권한 부여
        if (!userRepository.findByCustomerCode(user.getCustomerCode()).isEmpty()){
            user.setRole("MANAGER");
        }

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

    // 다중 role 지원
    public Page<User> getUsersByRole(List<String> roles, Pageable pageable) {
        return userRepository.findAllByRoleInOrderByIdDesc(roles, pageable);
    }

    // 다중 role 지원
    public Page<User> searchUsersByRoleAndKeyword(List<String> roles, String keyword, Pageable pageable) {
        return userRepository.findAllByRoleInAndUsernameContainingOrEmailContainingOrderByIdDesc(roles, keyword, keyword, pageable);
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
    // ADMIN을 제외한 역할 카운트
    public long getUsersCountByRoles(List<String> roles) {
        return userRepository.countByRoleIn(roles);
    }

    public Page<User> searchUsersByCustomerCode(String customerCode, String search, Pageable pageable) {
        return userRepository.searchByCustomerCodeAndNameOrTelOrEmail(customerCode,search,pageable);
    }

    public Page<User> getUsersByCustomerCode(String customerCode, Pageable pageable) {
        return userRepository.findAllByCustomerCode(customerCode,pageable);
    }

    public List<User> getUsersByCustomerCodeList(String customerCode) {
        return userRepository.findAllByCustomerCode(customerCode);
    }

    public Long getUsersCountByCustomerCode(String customerCode) {
        return userRepository.countByCustomerCode(customerCode);
    }

    public UserDto updateUser(UserDto userDto) {
        // userRepository에서 user 찾아서 정보 수정 후 저장
        User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new IllegalArgumentException("회원 정보가 존재하지 않습니다."));
        System.out.println("user name : " + user.getName());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setTel(userDto.getTel());
        user.setRole(userDto.getRole());
        return new UserDto(userRepository.save(user));

    }

    public void deleteUser(Long id) {
        // userRepository에서 user 삭제
        userRepository.deleteById(id);
    }

    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
                System.out.println("user name : " + user.getName());
        user.setDeleteFlag(true);
        userRepository.save(user);
    }

    public void restoreUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        user.setDeleteFlag(false);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Page<User> searchAllFieldsExcludeAdmin(String search, Pageable pageable) {
        return userRepository.searchAllFieldsExcludeAdmin(search, pageable);
    }
}
