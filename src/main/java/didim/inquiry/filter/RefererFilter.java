package didim.inquiry.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 모든 컨트롤러에서 inquiryList 요청이 발생하는걸 차단하기 위해 Filter추가
// 흐름 : /inquiryList의 Referer를 확인 -> null이면 차단하는 로직 추가 후 필터체인 필터에 추가 -> SecurityConfig에서 해당 필터 추가하여 사용
@Component
public class RefererFilter extends OncePerRequestFilter{

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if(requestURI.equals("/inquiryList")){
            String referer = request.getHeader("Referer");

            if (referer == null || referer.trim().isEmpty()) {
                return;
            }
        }

        filterChain.doFilter(request,response);
    }
}
