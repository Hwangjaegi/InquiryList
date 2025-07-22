package didim.inquiry.controller.absClass;

import didim.inquiry.domain.User;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public abstract class BaseController {

    @Autowired
    private UserService userService;

    protected User getCurrentUser(){
        String username = SecurityUtil.getCurrentUsername();
        if (username == null){
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }

        return userService.getUserByUsername(username);
    }
}
