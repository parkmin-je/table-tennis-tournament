package com.parkminje.pingpong.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 인증을 시도하기 "전에" 잠금 여부를 확인한다 (잠긴 동안은 올바른 비밀번호도 거부) */
@RequiredArgsConstructor
public class LoginAttemptFilter extends OncePerRequestFilter {

    public static final String LOGIN_URL = "/user/login";

    private final LoginAttemptService attempts;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && LOGIN_URL.equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long minutes = attempts.lockedMinutes(request.getRemoteAddr(), request.getParameter("username"));
        if (minutes > 0) {
            response.sendRedirect(request.getContextPath() + LOGIN_URL + "?locked=" + minutes);
            return;
        }
        chain.doFilter(request, response);
    }
}
