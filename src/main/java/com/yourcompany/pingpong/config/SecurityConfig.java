package com.yourcompany.pingpong.config;

import com.yourcompany.pingpong.common.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            /* ── 요청 권한 ── */
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(
                    "/", "/user/login", "/user/signup",
                    "/css/**", "/js/**", "/images/**",
                    "/favicon.ico", "/manifest.json",
                    "/api/search", "/api/player/stats/**"
                ).permitAll()
                .requestMatchers(
                    "/tournament/list", "/tournament/detail/**",
                    "/tournament/bracket/**", "/tournament/statistics/**",
                    "/tournament/mainBracket/**",
                    "/calendar", "/calendar/events",
                    "/player/list", "/player/ranking", "/player/{id}", "/player/detail/**",
                    "/club/list", "/club/detail/**",
                    "/board/list", "/board/detail/**",
                    "/match/mainBracketData/**", "/match/inProgress/**"
                ).permitAll()
                // 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(
                    "/tournament/create", "/tournament/edit/**", "/tournament/delete/**",
                    "/tournament/createMainBracket/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/player/create", "/player/edit/**", "/player/delete/**"
                ).hasRole("ADMIN")
                .requestMatchers("/club/create", "/club/edit/**", "/club/delete/**").hasRole("ADMIN")
                .requestMatchers(
                    "/match/start/**", "/match/complete/**", "/match/edit/**"
                ).hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            /* ── 보안 헤더 ── */
            .headers(headers -> headers
                .frameOptions(fo -> fo.sameOrigin())
                .contentTypeOptions(ct -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            /* ── 세션 관리 ── */
            .sessionManagement(session -> session
                .sessionFixation(sf -> sf.migrateSession())
                .maximumSessions(3)
                .maxSessionsPreventsLogin(false)
            )

            /* ── CSRF ── */
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**")
            )

            /* ── 로그인 ── */
            .formLogin(form -> form
                .loginPage("/user/login")
                .loginProcessingUrl("/user/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            )

            /* ── 로그아웃 ── */
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/user/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest req, HttpServletResponse res, Authentication auth) -> {
            String ip = getClientIp(req);
            loginAttemptService.loginSucceeded(ip);
            res.sendRedirect("/");
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) -> {
            String ip = getClientIp(req);
            loginAttemptService.loginFailed(ip);
            if (loginAttemptService.isBlocked(ip)) {
                int remaining = loginAttemptService.getRemainingMinutes(ip);
                res.sendRedirect("/user/login?blocked=true&minutes=" + remaining);
            } else {
                res.sendRedirect("/user/login?error=true");
            }
        };
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}