package com.parkminje.pingpong.user;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User signUp(String username, String rawPassword, String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("이미 가입된 이메일입니다.");
        }
        User user = userRepository.save(new User(username, passwordEncoder.encode(rawPassword), normalizedEmail, name.trim(), Role.USER));
        log.info("회원 가입: id={} username={}", user.getId(), username);
        return user;
    }

    public void updateProfile(Long userId, String name, String email) {
        User user = get(userId);
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }
        user.updateProfile(name.trim(), normalizedEmail);
    }

    public void changePassword(Long userId, String current, String next) {
        User user = get(userId);
        if (!passwordEncoder.matches(current, user.getPassword())) {
            throw new BusinessException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(next, user.getPassword())) {
            throw new BusinessException("새 비밀번호가 현재 비밀번호와 같습니다.");
        }
        user.changePassword(passwordEncoder.encode(next));
        log.info("비밀번호 변경: userId={}", userId);
    }

    /** 관리자: 권한 변경 (마지막 관리자 강등 방지) */
    public void changeRole(Long actorId, Long userId, Role role) {
        User user = get(userId);
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException("마지막 관리자는 권한을 낮출 수 없습니다.");
        }
        if (actorId.equals(userId) && role != Role.ADMIN) {
            throw new BusinessException("본인의 관리자 권한은 직접 해제할 수 없습니다.");
        }
        user.changeRole(role);
        log.info("권한 변경: actor={} user={} role={}", actorId, userId, role);
    }

    public void setEnabled(Long actorId, Long userId, boolean enabled) {
        if (actorId.equals(userId)) {
            throw new BusinessException("본인 계정은 비활성화할 수 없습니다.");
        }
        User user = get(userId);
        if (!enabled && user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException("마지막 관리자는 비활성화할 수 없습니다.");
        }
        user.setEnabled(enabled);
    }

    /** 관리자: 임시 비밀번호 발급 */
    public String resetPassword(Long userId) {
        User user = get(userId);
        String temp = TemporaryPassword.generate();
        user.changePassword(passwordEncoder.encode(temp));
        log.info("임시 비밀번호 발급: userId={}", userId);
        return temp;
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("회원", id));
    }

    @Transactional(readOnly = true)
    public Page<User> search(String q, int page) {
        return userRepository.search(q == null ? "" : q.trim(), PageRequest.of(Math.max(page, 0), 30));
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }
}
