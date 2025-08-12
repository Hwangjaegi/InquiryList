package didim.inquiry.service;

import didim.inquiry.domain.Customer;
import didim.inquiry.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("고객코드를 찾을 수 없습니다."));
    }

    public Customer getCustomerByCode(String customerCode) {
        return customerRepository.findByCode(customerCode);
    }

    public boolean existsByCustomerCodeAndStatusActive(String customerCode) {
        return customerRepository.existsByCodeAndStatus(customerCode,"ACTIVE");
    }

    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    // active인 고객코드 조회
    public List<Customer> findAllByActive(String active) {
        return customerRepository.findAllByStatus(active);
    }
    
    // 여러 고객코드의 상태 확인
    public List<Customer> findAllByCodeInAndStatus(Set<String> codes, String status) {
        return customerRepository.findByCodeInAndStatus(new ArrayList<>(codes), status);
    }
}