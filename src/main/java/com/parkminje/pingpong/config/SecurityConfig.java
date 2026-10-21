package com.parkminje.pingpong.config;

import com.parkminje.pingpong.security.LoginAttemptFilter;
import com.parkminje.pingpong.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.util.LinkedHashMap;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self' ws: wss:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService attempts, SessionRegistry sessionRegistry) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/vendor/**", "/favicon.ico", "/favicon.svg", "/manifest.json").permitAll()
                .requestMatchers("/error", "/actuator/health/**", "/ws/**").permitAll()
                .requestMatchers("/user/login", "/user/signup").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manage/**").hasRole("STAFF")
                .requestMatchers(HttpMethod.GET,
                        "/", "/tournaments", "/tournament/*", "/tournament/*/bracket", "/tournament/*/board",
                        "/tournament/*/live", "/tournament/*/fragment/**",
                        "/players", "/player/*", "/ranking", "/clubs", "/club/*",
                        "/board", "/board/*", "/calendar", "/api/calendar", "/api/search").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new LoginAttemptFilter(attempts), UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/user/login")
                .loginProcessingUrl("/user/login")
                .successHandler(successHandler(attempts))
                .failureHandler(failureHandler(attempts))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/user/logout")
                .logoutSuccessUrl("/?logout")
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint()))
            .sessionManagement(s -> s
                .sessionFixation(f -> f.migrateSession())
                .maximumSessions(5)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/user/login?expired"))
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .frameOptions(f -> f.sameOrigin())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    /** API 는 401, 화면은 로그인 페이지로 */
    private AuthenticationEntryPoint entryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> map = new LinkedHashMap<>();
        map.put(PathPatternRequestMatcher.withDefaults().matcher("/api/**"), new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(map);
        delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/user/login"));
        return delegating;
    }

    private SavedRequestAwareAuthenticationSuccessHandler successHandler(LoginAttemptService attempts) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                attempts.recordSuccess(request.getRemoteAddr(), authentication.getName());
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
        handler.setDefaultTargetUrl("/");
        return handler;
    }

    private AuthenticationFailureHandler failureHandler(LoginAttemptService attempts) {
        return (request, response, exception) -> {
            String ip = request.getRemoteAddr();
            String username = request.getParameter("username");
            if (exception instanceof DisabledException) {
                response.sendRedirect(request.getContextPath() + "/user/login?disabled");
                return;
            }
            attempts.recordFailure(ip, username);
            long locked = attempts.lockedMinutes(ip, username);
            response.sendRedirect(request.getContextPath() + (locked > 0 ? "/user/login?locked=" + locked : "/user/login?error"));
        };
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
