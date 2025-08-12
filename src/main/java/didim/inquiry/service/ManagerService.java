package didim.inquiry.service;

import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.repository.ManagerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagerService {
    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    // 이메일 중복 확인 있으면 true
    public boolean existsByEmail(String email) {
        return managerRepository.existsByEmail(email);
    }

    // 특정 담당자를 제외하고 이메일 중복 확인
    public boolean existsByEmailExcludingManager(String email, Long excludeManagerId) {
        return managerRepository.existsByEmailAndIdNot(email, excludeManagerId);
    }

    // 담당자 추가 생성    
    public Manager createManager(String name, String tel, String email, User currentUser) {
        Manager manager = new Manager();
        manager.setName(name);
        manager.setTel(tel);
        manager.setEmail(email);
        manager.setUser(currentUser);
        manager.setDeleteFlag(false);
        System.out.println("manager Repository : " + manager.getName());

        return managerRepository.save(manager);
    }

    public List<Manager> findByUserId(Long userId) {
        return managerRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    // ID로 매니저 조회
    public Manager getManagerById(Long managerId) {
        return managerRepository.findById(managerId).orElse(null);
    }
    
    // 특정 사용자 ID로 매니저 페이징 조회
    public Page<Manager> getManagersByUserId(Long userId, Pageable pageable) {
        return managerRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    // 특정 사용자 ID로 매니저 검색
    public Page<Manager> searchManagersByUserId(Long userId, String keyword, Pageable pageable) {
        return managerRepository.findByUserIdAndKeyword(userId, keyword, pageable);
    }
    
    // 특정 사용자 ID로 매니저 개수 조회
    public long getManagerCountByUserId(Long userId) {
        return managerRepository.countByUserId(userId);
    }

    // 담당자 수정
    @Transactional
    public Manager updateManager(Long id, String name, String tel, String email, User currentUser) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다."));
        
        // 권한 확인 (현재 사용자가 담당자의 소유자인지 확인)
        if (!manager.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("담당자를 수정할 권한이 없습니다.");
        }
        
        // 삭제된 담당자는 수정 불가
        if (manager.isDeleteFlag()) {
            throw new RuntimeException("삭제된 담당자는 수정할 수 없습니다.");
        }
        
        // 이메일 중복 확인 (자신의 이메일은 제외)
        if (!email.equals(manager.getEmail()) && managerRepository.existsByEmailAndIdNot(email, manager.getId())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }
        
        manager.setName(name);
        manager.setTel(tel);
        manager.setEmail(email);
        
        return managerRepository.save(manager);
    }

    // 담당자 삭제 (소프트 삭제)
    @Transactional
    public void deleteManager(Long id, User currentUser) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다."));
        
        // 권한 확인 (현재 사용자가 담당자의 소유자인지 확인)
        if (!manager.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("담당자를 삭제할 권한이 없습니다.");
        }
        
        // 이미 삭제된 담당자인지 확인
        if (manager.isDeleteFlag()) {
            throw new RuntimeException("이미 삭제된 담당자입니다.");
        }
        
        manager.setDeleteFlag(true);
        managerRepository.save(manager);
    }

    // 담당자 복원
    @Transactional
    public void restoreManager(Long id, User currentUser) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다."));
        
        // 권한 확인 (현재 사용자가 담당자의 소유자인지 확인)
        if (!manager.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("담당자를 복원할 권한이 없습니다.");
        }
        
        // 삭제되지 않은 담당자인지 확인
        if (!manager.isDeleteFlag()) {
            throw new RuntimeException("삭제되지 않은 담당자입니다.");
        }
        
        manager.setDeleteFlag(false);
        managerRepository.save(manager);
    }
}
