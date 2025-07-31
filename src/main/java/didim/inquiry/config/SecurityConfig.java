package didim.inquiry.config;

import didim.inquiry.filter.RefererFilter;
import didim.inquiry.security.JwtAuthenticationFilter;
import didim.inquiry.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RefererFilter refererFilter;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    // 방법 1: HttpSecurity 사용 (and() 제거)
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());

        return authenticationManagerBuilder.build();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 최신 스타일로 CSRF 비활성화
                .addFilterBefore(refererFilter, UsernamePasswordAuthenticationFilter.class)  // RefererFilter 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)  // JWT 필터 추가
                .authorizeHttpRequests(auth -> auth
                        // 공개 접근 가능한 경로들 (JWT 검증 불필요)
                        .requestMatchers("/api/auth/**", "/api/check-*", "/signup", "/login", "/css/**", "/js/**", 
                                       "/image/**", "/temp/**", "/posts/**", "/uploads/**").permitAll()
                        // 인증이 필요한 경로들 (JWT 또는 세션 인증 필요)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/signin")
                        .defaultSuccessUrl("/inquiryList",true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(10)  // 최대 세션 수 제한
                        .maxSessionsPreventsLogin(false)  // 기존 세션 무효화
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")       // /logout url요청을 가로채서 세션 삭제
                        .logoutSuccessUrl("/login") // 세션삭제 성공 시 이동할 url 요청
                        .deleteCookies("jwt_token", "JSESSIONID")  // JWT 토큰과 세션 쿠키 삭제
                        .invalidateHttpSession(true)  // HTTP 세션 무효화
                        .clearAuthentication(true)   // 인증 정보 클리어
                        .permitAll()
                )
                .userDetailsService(customUserDetailsService);
        return http.build();
    }
}