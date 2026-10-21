package com.parkminje.pingpong.bootstrap;

import com.parkminje.pingpong.config.AppProperties;
import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import com.parkminje.pingpong.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정이 하나도 없을 때만, 환경변수(ADMIN_PASSWORD)로 받은 비밀번호로 최초 관리자를 만든다.
 * 비밀번호를 코드나 로그에 남기지 않는다.
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }
        AppProperties.BootstrapAdmin admin = props.bootstrapAdmin();
        if (admin.password() == null || admin.password().isBlank()) {
            if (!props.sampleData()) {
                log.warn("관리자 계정이 없습니다. ADMIN_PASSWORD 환경변수를 지정하고 다시 시작하면 '{}' 계정이 생성됩니다.", admin.username());
            }
            return;
        }
        if (admin.password().length() < 10) {
            throw new IllegalStateException("ADMIN_PASSWORD 는 10자 이상이어야 합니다.");
        }
        if (userRepository.existsByUsername(admin.username())) {
            userRepository.findByUsername(admin.username()).ifPresent(u -> u.changeRole(Role.ADMIN));
            log.info("기존 회원 '{}' 을(를) 관리자로 지정했습니다.", admin.username());
            return;
        }
        userRepository.save(new User(admin.username(), passwordEncoder.encode(admin.password()), admin.email(), "관리자", Role.ADMIN));
        log.info("최초 관리자 계정 '{}' 을(를) 생성했습니다.", admin.username());
    }
}
