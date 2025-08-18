package didim.inquiry;

import didim.inquiry.domain.Inquiry;
import didim.inquiry.domain.User;
import didim.inquiry.service.AdminService;
import didim.inquiry.service.InquiryService;
import didim.inquiry.service.UserService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@Transactional
class InquiryApplicationTests {

	private static final Logger log = LoggerFactory.getLogger(InquiryApplicationTests.class);
	private final UserService userService;
	private final InquiryService inquiryService;
	private final AdminService adminService;
	@Autowired
    InquiryApplicationTests(UserService userService, InquiryService inquiryService, AdminService adminService) {
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.adminService = adminService;
    }



	@Test
	void 회원가입() {
		// given
		User user = new User();
		user.setUsername("username2");
		user.setCustomerCode("ARA");
		user.setPassword("7977");

		// when
		boolean response = userService.signUpUser(user);

	}

}
