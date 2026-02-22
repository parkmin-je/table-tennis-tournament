package com.yourcompany.pingpong.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 브루트 포스 방어 서비스
 * - IP 기반으로 실패 횟수를 추적
 * - MAX_ATTEMPTS 초과 시 LOCK_DURATION 동안 차단
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 10;
    private static final int LOCK_MINUTES = 15;

    private record AttemptInfo(int count, LocalDateTime lastAttempt) {}

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public void loginFailed(String ip) {
        AttemptInfo info = attempts.getOrDefault(ip, new AttemptInfo(0, LocalDateTime.now()));
        int newCount = info.count() + 1;
        attempts.put(ip, new AttemptInfo(newCount, LocalDateTime.now()));
        if (newCount >= MAX_ATTEMPTS) {
            log.warn("SECURITY: IP {} has been blocked after {} failed login attempts", ip, newCount);
        }
    }

    public void loginSucceeded(String ip) {
        attempts.remove(ip);
    }

    public boolean isBlocked(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return false;
        if (info.count() < MAX_ATTEMPTS) return false;
        // 잠금 시간이 지났으면 해제
        if (info.lastAttempt().isBefore(LocalDateTime.now().minusMinutes(LOCK_MINUTES))) {
            attempts.remove(ip);
            return false;
        }
        return true;
    }

    public int getRemainingMinutes(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return 0;
        long elapsed = java.time.Duration.between(info.lastAttempt(), LocalDateTime.now()).toMinutes();
        return (int) Math.max(0, LOCK_MINUTES - elapsed);
    }
}
