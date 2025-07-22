package didim.inquiry.config;

import didim.inquiry.filter.CustomAuthenticationProvider;
import didim.inquiry.filter.RefererFilter;
import didim.inquiry.filter.UserAuthenticationFilter;
import didim.inquiry.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RefererFilter refererFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // 기존 시큐리티 로그인방식에서 고객코드 추가 적용
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception{
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
        builder.authenticationProvider(new CustomAuthenticationProvider(customUserDetailsService, passwordEncoder()));
        return builder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        UserAuthenticationFilter loginFilter = new UserAuthenticationFilter();
        loginFilter.setAuthenticationManager(authenticationManager(http));
        loginFilter.setFilterProcessesUrl("/signin"); // 로그인 요청을 가로채서 필터 적용
        loginFilter.setAuthenticationFailureHandler(((request, response, exception) -> {
            response.sendRedirect("/login?error=true");
        }));
        loginFilter.setAuthenticationSuccessHandler(((request, response, authentication) -> {
            response.sendRedirect("/inquiryList");
        }));

        http
                .csrf(csrf -> csrf.disable())  // 최신 스타일로 CSRF 비활성화
                .addFilterBefore(refererFilter, UsernamePasswordAuthenticationFilter.class)  // RefererFilter 추가
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/signup", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(10)  // 최대 세션 수 제한
                        .maxSessionsPreventsLogin(false)  // 기존 세션 무효화
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")       // /logout url요청을 가로채서 세션 삭제
                        .logoutSuccessUrl("/login") // 세션삭제 성공 시 이동할 url 요청
                        .permitAll()
                )
                .userDetailsService(customUserDetailsService);
        return http.build();
    }
}