package didim.inquiry.common;

import didim.inquiry.domain.User;
import didim.inquiry.security.SecurityUtil;
import didim.inquiry.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 공통 컨트롤러 기본 클래스
 * 모든 컨트롤러에서 공통으로 사용하는 기능들을 제공합니다.
 */
public abstract class BaseController {

    @Autowired
    private UserService userService;

    /**
     * 현재 로그인한 사용자 정보를 반환합니다.
     * @return 현재 사용자 정보
     * @throws UsernameNotFoundException 로그인이 필요한 경우
     */
    protected User getCurrentUser(){
        String username = SecurityUtil.getCurrentUsername();
        if (username == null){
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }

        return userService.getUserByUsername(username);
    }
}
