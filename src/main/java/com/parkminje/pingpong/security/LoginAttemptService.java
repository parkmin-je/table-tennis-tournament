package com.parkminje.pingpong.security;

import com.parkminje.pingpong.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 무차별 대입 방어.
 * IP 기준과 아이디 기준을 따로 센다 — IP를 바꿔가며 한 계정을 노리는 경우와
 * 한 IP에서 여러 계정을 시도하는 경우를 모두 막는다.
 * (IP 는 request.getRemoteAddr() 기준. 프록시 뒤라면 server.forward-headers-strategy 로 신뢰할 프록시만 반영)
 */
@Slf4j
@Service
public class LoginAttemptService {

    private record Attempt(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(AppProperties props) {
        this(props.security().loginMaxAttempts(), Duration.ofMinutes(props.security().loginLockMinutes()), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public void recordFailure(String ip, String username) {
        bump("ip:" + ip);
        if (username != null && !username.isBlank()) {
            bump("user:" + username.toLowerCase());
        }
    }

    public void recordSuccess(String ip, String username) {
        attempts.remove("ip:" + ip);
        if (username != null) {
            attempts.remove("user:" + username.toLowerCase());
        }
    }

    /** 잠금 상태면 남은 시간(분, 최소 1), 아니면 0 */
    public long lockedMinutes(String ip, String username) {
        long byIp = remaining("ip:" + ip);
        long byUser = username == null || username.isBlank() ? 0 : remaining("user:" + username.toLowerCase());
        return Math.max(byIp, byUser);
    }

    private void bump(String key) {
        Attempt next = attempts.compute(key, (k, a) -> {
            Instant now = clock.instant();
            if (a == null || isExpired(a, now)) {
                return new Attempt(1, now);
            }
            return new Attempt(a.failures() + 1, now);
        });
        if (next.failures() == maxAttempts) {
            log.warn("로그인 잠금: {} ({}회 실패)", key, next.failures());
        }
    }

    private long remaining(String key) {
        Attempt a = attempts.get(key);
        if (a == null || a.failures() < maxAttempts) {
            return 0;
        }
        Duration left = Duration.between(clock.instant(), a.lastFailure().plus(lockDuration));
        if (left.isNegative() || left.isZero()) {
            attempts.remove(key);
            return 0;
        }
        return Math.max(1, (left.toSeconds() + 59) / 60);
    }

    private boolean isExpired(Attempt a, Instant now) {
        return a.lastFailure().plus(lockDuration).isBefore(now);
    }

    /** 메모리 누수 방지: 만료된 기록 정리 */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void purge() {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(e -> isExpired(e.getValue(), now));
    }
}
