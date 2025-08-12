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
    public boolean signUpUser(User user) {
        // 고객코드가 생성되어있고 활성화 되어있는지 확인 있으면 true
        boolean exists = adminRepository.existsByCodeAndStatus(user.getCustomerCode(), "ACTIVE");
        if (!exists) {
            return false;
        }

        // user에서 아이디가 중복되면 가입 못하게 방지
        if (userRepository.findByUsername(user.getUsername()).isPresent()) { //isPresent : 객체의 값이 존재하는지 확인
            return false;
        }

        // 비밀번호가 8자리 미만이면 가입 방지
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return false;
        }

        // email이 중복되면 가입 못하게 방지
        if (userRepository.findByEmail(user.getEmail()).isPresent()) { //isPresent : 객체의 값이 존재하는지 확인
            return false;
        }

//        // 고객코드로 처음 가입한 사람은 관리자 권한 부여
//        if (userRepository.findByCustomerCode(user.getCustomerCode()).isEmpty()) {
//            if (user.getUsername().equals("admin") && user.getCustomerCode().equals("D000001")) {
//                user.setRole("ADMIN");
//            } else {
//                user.setRole("MANAGER");
//            }
//        }

        //가입된 정보가 없을 경우 패스워드 암호화 처리 후 가입처리
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return true;
    }

    // 관리자 최소정보 회원가입 (이름, 이메일, 전화번호 공란 허용)
    public boolean signUpUserAllowBlank(User user) {
        // 고객코드가 생성되어있고 활성화 되어있는지 확인
        boolean exists = adminRepository.existsByCodeAndStatus(user.getCustomerCode(), "ACTIVE");
        if (!exists) {
            return false;
        }
        // 아이디 중복 방지
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false;
        }
        // 비밀번호 8자리 이상
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return false;
        }
        // 이메일이 비어있지 않으면 중복 체크
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return false;
            }
        }
        
        // USER 역할인 경우: 고객코드당 1개만 허용
        if ("USER".equals(user.getRole())) {
            if (userRepository.findByCustomerCodeAndRole(user.getCustomerCode(), "USER").isPresent()) {
                return false;
            }
        }
        
        // 패스워드 암호화 후 저장
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return true;
    }

    //2. id를 통해 회원정보 조회
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 3. 계정 아이디(username)를 통해 회원정보 조회
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("로그인한 계정 정보가 존재하지 않습니다."));
    }

    // 4. 계정 아이디(username)와 고객코드(customerCode)를 통해 회원정보 조회
    public User getUserByUsernameAndCustomerCode(String username, String customerCode) {
        return userRepository.findByUsernameAndCustomerCode(username, customerCode).orElseThrow(() -> new IllegalArgumentException("로그인한 계정 정보가 존재하지 않습니다."));
    }

    // 다중 role 지원
    public Page<User> getUsersByRole(List<String> roles, Pageable pageable) {
        return userRepository.findAllByRoleInOrderByIdDesc(roles, pageable);
    }

    // 다중 role 지원
    public Page<User> searchUsersByRoleAndKeyword(List<String> roles, String keyword, Pageable pageable) {
        return userRepository.findAllByRoleInAndUsernameContainingOrEmailContainingOrderByIdDesc(roles, keyword, keyword, pageable);
    }

    // 현재 사용자를 제외한 모든 사용자 조회
    public Page<User> getAllUsersExceptCurrent(Long currentUserId, Pageable pageable) {
        return userRepository.findAllByIdNotOrderByIdDesc(currentUserId, pageable);
    }

    // 현재 사용자를 제외하고 검색
    public Page<User> searchAllUsersExceptCurrent(Long currentUserId, String keyword, Pageable pageable) {
        return userRepository.searchAllFieldsExceptUser(currentUserId, keyword, pageable);
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
        return userRepository.searchByCustomerCodeAndNameOrTelOrEmail(customerCode, search, pageable);
    }

    public Page<User> getUsersByCustomerCode(String customerCode, Pageable pageable) {
        return userRepository.findAllByCustomerCode(customerCode, pageable);
    }

    public List<User> getUsersByCustomerCodeList(String customerCode) {
        return userRepository.findAllByCustomerCode(customerCode);
    }

    public Long getUsersCountByCustomerCode(String customerCode) {
        return userRepository.countByCustomerCode(customerCode);
    }

    public UserDto updateUserWithPassword(UserDto userDto , String newPassword , String confirmPassword) {
        //이부분 수정필요

        User user = userRepository.findByUsername(userDto.getUsername()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        // 이메일 중복 방지
        if (!userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
            }
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(userDto.getCurrentPassword(),user.getPassword())){
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호가 8자리 미만이면 가입 방지
        if (newPassword == null || newPassword.length() < 8) {
            System.out.println("userdto : " + userDto.getPassword());
            System.out.println("leng : " + userDto.getPassword().length());
            throw new IllegalArgumentException("비밀번호가 8자리 미만입니다.");
        }

        // 비밀번호 확인 일치 검증
        if (!newPassword.equals(confirmPassword)){
            throw  new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }


        user.setName(userDto.getName());
        user.setTel(userDto.getTel());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(newPassword));

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

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Page<User> searchAllFieldsExcludeAdmin(String search, Pageable pageable) {
        return userRepository.searchAllFieldsExcludeAdmin(search, pageable);
    }

    // 관리자 -> 유저 권한변경
    public UserDto updateRole(UserDto userDto) {
        userRepository.updateByRole(userDto.getId(), userDto.getRole());
        // 변경된 유저 정보 반환
        User updatedUser = userRepository.findById(userDto.getId()).orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        return new UserDto(updatedUser);
    }

    // 비밀번호 변경 없이 이름, 전화번호, 이메일만 수정
    public void updateUserInfoOnly(UserDto userDto) {
        User user = userRepository.findByUsername(userDto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        user.setName(userDto.getName());
        user.setTel(userDto.getTel());
        user.setEmail(userDto.getEmail());
        userRepository.save(user);
    }

    // 고객코드 존재 여부 확인 (삭제되지 않은 사용자만)
    public boolean existsByCustomerCode(String customerCode) {
        return userRepository.existsByCustomerCodeAndDeleteFlagFalse(customerCode);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 고객코드 상태 확인 (ACTIVE가 아니면 false)
    public boolean isCustomerCodeActive(String customerCode) {
        return adminRepository.existsByCodeAndStatus(customerCode, "ACTIVE");
    }

    // 관리자용 사용자 정보 수정
    @Transactional
    public void updateUserByAdmin(UserDto userDto) {
        User user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // username 중복 체크 (자신 제외)
        if (!user.getUsername().equals(userDto.getUsername())) {
            if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
        }

        // email 중복 체크 (자신 제외, 빈 값이 아닌 경우만)
        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            if (!userDto.getEmail().equals(user.getEmail())) {
                if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
            }
        }

        // 사용자 정보 업데이트
        user.setUsername(userDto.getUsername());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setTel(userDto.getTel());
        user.setRole(userDto.getRole());

        userRepository.save(user);
    }
}
