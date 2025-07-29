package didim.inquiry.service;

import didim.inquiry.domain.Manager;
import didim.inquiry.domain.User;
import didim.inquiry.repository.ManagerRepository;
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
        return managerRepository.findAllByUserId(userId);
    }

    // ID로 매니저 조회
    public Manager getManagerById(Long managerId) {
        return managerRepository.findById(managerId).orElse(null);
    }
}
