package didim.inquiry.config;

import didim.inquiry.domain.Customer;
import didim.inquiry.domain.User;
import didim.inquiry.service.CustomerService;
import didim.inquiry.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Bean
    CommandLineRunner initDatabase(CustomerService customerService, UserService userService, PasswordEncoder passwordEncoder) {
        return args -> {
            // 트랜잭션 시작
            initData(customerService, userService, passwordEncoder);
        };
    }

    //서버 실행 시 고객코드에 DIDIM이 존재하지 않을경우 생성 , User에 DIDIM 고객코드로 생성된 유저가 없을경우 생성
    @Transactional
    public void initData(CustomerService customerService, UserService userService, PasswordEncoder passwordEncoder) {
        // Customer 초기화
        if (!customerService.existsByCustomerCodeAndStatusActive("DIDIM")) {
            Customer customer = new Customer();
            customer.setCode("DIDIM");
            customer.setCompany("디딤솔루션");
            customerService.saveCustomer(customer);
            log.info("Inserted initial customer: code=DIDIM, company=디딤솔루션");
        } else {
            log.info("Customer with code DIDIM already exists, skipping initialization");
        }

        // User 초기화
        if (!userService.existsByCustomerCode("DIDIM")) {
            User user = new User();
            user.setCustomerCode("DIDIM");
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("didim!7977")); // 비밀번호 암호화
            user.setEmail("system@didimsolution.com");
            user.setName("디딤솔루션");
            user.setRole("ADMIN");
            user.setTel("010-1234-1234");
            user.setDeleteFlag(false);
            userService.save(user);
            log.info("Inserted initial user: username=admin, customerCode=DIDIM");
        } else {
            log.info("User with customerCode DIDIM and username admin already exists, skipping initialization");
        }
    }
}