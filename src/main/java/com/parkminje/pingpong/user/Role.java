package com.parkminje.pingpong.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("일반 회원"),
    STAFF("운영진"),
    ADMIN("관리자");

    private final String label;

    public String authority() {
        return "ROLE_" + name();
    }
}
