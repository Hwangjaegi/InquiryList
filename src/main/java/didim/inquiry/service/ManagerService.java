package didim.inquiry.service;

import didim.inquiry.domain.Manager;
import didim.inquiry.dto.ManagerDto;
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

    //일반 관리자 리스트
    public List<Manager> getManagerList(Long user_id) {
        //user_id를 통해 해당 유저에서 추가한 관리자리스트를 가져온다.
        return managerRepository.findByUserIdAndDeleteFlagFalseOrderByCreatedAtDesc(user_id);
    }

    //페이징 관리자 리스트
    public Page<Manager> getManagerList(Long user_id , String search , Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            //user_id를 통해 해당 유저에서 추가한 관리자리스트를 가져온다.
            return managerRepository.searchByUserIdAndNameOrEmail(user_id , search , pageable);
        }else {
            return managerRepository.findByUserIdAndDeleteFlagFalseOrderByCreatedAtDesc(user_id , pageable);
        }

    }

    public Manager addManager(ManagerDto managerDto) {
        Manager manager = new Manager();
        manager.setUser(managerDto.getUser());
        manager.setName(managerDto.getManagerName());
        manager.setTel(managerDto.getManagerTel());
        manager.setEmail(managerDto.getManagerEmail());
        return managerRepository.save(manager);
    }

    public Manager getSelectedManager(long managerId) {
        return managerRepository.findById(managerId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));
    }

    public Manager getManagerById(Long id) {
        return managerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("id에 해당하는 관리자가 존재하지 않습니다."));
    }

    @Transactional
    public void deleteManagerById(Long id) {
        managerRepository.updateManagerDeleteFlagById(id);
    }

    @Transactional
    public ManagerDto updateManager(ManagerDto managerDto) {
        Manager manager = managerRepository.findById(managerDto.getManagerId()).orElseThrow(() -> new IllegalArgumentException("id에 해당하는 관리자가 존재하지 않습니다."));
        manager.setName(managerDto.getManagerName());
        manager.setEmail(managerDto.getManagerEmail());
        manager.setTel(managerDto.getManagerTel());

        return new ManagerDto(manager);
    }

    public Long getManagerCount() {
        return managerRepository.count();
    }
}
