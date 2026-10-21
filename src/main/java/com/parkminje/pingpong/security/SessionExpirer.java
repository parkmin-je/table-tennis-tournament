package com.parkminje.pingpong.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/** 권한 변경·계정 비활성화 시 해당 회원의 로그인 세션을 만료시켜 즉시 반영한다 */
@Component
@RequiredArgsConstructor
public class SessionExpirer {

    private final SessionRegistry sessionRegistry;

    public void expireUser(Long userId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserPrincipal p && p.getId().equals(userId)) {
                sessionRegistry.getAllSessions(p, false).forEach(SessionInformation::expireNow);
            }
        }
    }
}
