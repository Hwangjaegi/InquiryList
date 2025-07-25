package didim.inquiry.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtil {

    //시큐리티 세션에 등록된 유저아이디 가져오는 공통 사용 메서드
    public static String getCurrentUsername(){
        //현재 사용자의 id를 가져온다
        //사용자 로그인 후 사용자가 문의한 문의리스트를 조회하여 표시 , 세션에서 가져오므로 쿼리실행 x
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null){
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails){ //꺼낸 사용자정보가 사용자 상세정보 타입일 경우
                username = ((UserDetails) principal).getUsername();
            }else if(principal instanceof String){
                username = (String) principal; //꺼낸 사용자정보가 문자열일경우 문자열로 가져오기
            }
        }

        return username;
    }
}
