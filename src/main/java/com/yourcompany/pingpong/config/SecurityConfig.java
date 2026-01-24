package com.yourcompany.pingpong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ⭐⭐⭐ 1순위: 에러 페이지 허용 ⭐⭐⭐
                        .requestMatchers("/error").permitAll()

                        // ⭐⭐⭐ 2순위: WebSocket 허용 ⭐⭐⭐
                        .requestMatchers("/ws/**").permitAll()

                        // ⭐⭐⭐ 3순위: 정적 리소스 허용 ⭐⭐⭐
                        .requestMatchers(
                                "/",
                                "/user/login",
                                "/user/signup",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ⭐⭐⭐ 4순위: 일반 사용자 접근 가능 (로그인 불필요) ⭐⭐⭐
                        .requestMatchers(
                                "/tournament/list",           // 대회 목록
                                "/tournament/detail/**",      // 대회 상세
                                "/tournament/bracket/**",     // 대진표 보기
                                "/tournament/statistics/**",  // 대회 통계
                                "/tournament/mainBracket/**", // 본선 대진표 데이터
                                "/calendar",                  // 대회 캘린더
                                "/calendar/events",           // 캘린더 이벤트 데이터
                                "/player/list",               // 선수 목록
                                "/player/ranking",            // 선수 랭킹
                                "/player/{id}",               // 선수 상세
                                "/club/list",                 // 클럽 목록
                                "/board/list",                // ✅ 게시판 목록 추가
                                "/board/detail/**",           // ✅ 게시글 상세 추가
                                "/match/mainBracketData/**",  // 대진표 JSON 데이터
                                "/match/inProgress/**"        // 진행 중 경기 목록
                        ).permitAll()

                        // ⭐⭐⭐ 5순위: 관리자 전용 ⭐⭐⭐
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/tournament/create").hasRole("ADMIN")
                        .requestMatchers("/tournament/edit/**").hasRole("ADMIN")
                        .requestMatchers("/tournament/delete/**").hasRole("ADMIN")
                        .requestMatchers("/tournament/createMainBracket/**").hasRole("ADMIN")
                        .requestMatchers("/player/create").hasRole("ADMIN")
                        .requestMatchers("/player/edit/**").hasRole("ADMIN")
                        .requestMatchers("/player/delete/**").hasRole("ADMIN")
                        .requestMatchers("/club/create").hasRole("ADMIN")
                        .requestMatchers("/club/delete/**").hasRole("ADMIN")
                        .requestMatchers("/match/start/**").hasRole("ADMIN")
                        .requestMatchers("/match/complete/**").hasRole("ADMIN")

                        // ⭐⭐⭐ 6순위: 나머지는 로그인 필요 ⭐⭐⭐
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**")  // ⭐ WebSocket CSRF 예외
                )
                .formLogin(form -> form
                        .loginPage("/user/login")
                        .loginProcessingUrl("/user/login")
                        .defaultSuccessUrl("/", true)  // ✅ 메인 화면으로 변경
                        .failureUrl("/user/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/user/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}