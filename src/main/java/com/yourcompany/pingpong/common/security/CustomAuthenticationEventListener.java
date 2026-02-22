package com.yourcompany.pingpong.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String ip = getClientIp();
        loginAttemptService.loginSucceeded(ip);
        log.info("LOGIN_SUCCESS: user={} ip={}", event.getAuthentication().getName(), ip);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String ip = getClientIp();
        loginAttemptService.loginFailed(ip);
        log.warn("LOGIN_FAILURE: user={} ip={} reason={}",
                event.getAuthentication().getName(), ip,
                event.getException().getMessage());
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
