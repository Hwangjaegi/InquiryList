package didim.inquiry.service;

import didim.inquiry.domain.Customer;
import didim.inquiry.dto.CustomerDto;
import didim.inquiry.repository.AdminRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    //고객코드 생성을 위해 dto에서 필요한 값을 꺼내 Entity 주입
    public void createCustomerCode(CustomerDto customerDto) {
        //코드가 데이터베이스에 존재하는지 확인
        boolean exists = adminRepository.existsByCode(customerDto.getCode());

        //존재하면 true 없으면 false가 반환
        if (exists) {
            //존재하면 예외를 발생시킨다 -> e.message에 정의한 메시지가 담긴다. -> try_Catch로 처리한다.
            throw new IllegalArgumentException("이미 존재하는 고객 코드입니다.");
        }

        Customer customer = new Customer();
        customer.setCode(customerDto.getCode());
        customer.setCompany(customerDto.getCompany());
        adminRepository.save(customer);
    }

    //어드민콘솔 고객코드관리 리스트 가져오기
    public Page<Customer> getCustomerCodeList(String search , Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return adminRepository.findByCodeContainingIgnoreCaseOrCompanyContainingIgnoreCase(search , search , pageable);
        } else {
            return adminRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    //고객코드 수정
    @Transactional
    public void updateCustomerCode(CustomerDto customerDto) {
        //1. 기존 Customer 엔티티조회
        Customer customer = adminRepository.findById(customerDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 고객이 존재하지 않습니다."));

        //2. 필드 수정
        customer.setCode(customerDto.getCode());
        customer.setCompany(customerDto.getCompany());
        customer.setStatus(customerDto.getStatus());
    }

//    //고객코드 삭제
//    @Transactional
//    public void deleteCustomerCode(Long id) {
//        adminRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("해당 고객이 존재하지 않습니다."));
//
//        adminRepository.deleteById(id);
//    }

    public long getActiveCustomerCount() {
        return adminRepository.countByStatusIgnoreCase("ACTIVE");
    }

    public long getNewCustomerCountByMonth(YearMonth currentMonth) {
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        return adminRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
    }

    public long getCustomerCount() {
        return adminRepository.count();
    }
}
