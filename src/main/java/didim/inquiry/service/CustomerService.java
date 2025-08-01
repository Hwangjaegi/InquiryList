package didim.inquiry.service;

import didim.inquiry.domain.Customer;
import didim.inquiry.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}