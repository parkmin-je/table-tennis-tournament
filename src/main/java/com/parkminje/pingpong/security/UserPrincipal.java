package com.parkminje.pingpong.security;

import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 세션에 저장되는 로그인 정보 (엔티티 대신 필요한 값만 보관) */
@Getter
public class UserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final String name;
    private final Role role;
    private final boolean enabled;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(Role.USER.authority()));
        if (role == Role.STAFF || role == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority(Role.STAFF.authority()));
        }
        if (role == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority(Role.ADMIN.authority()));
        }
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isStaff() {
        return role == Role.STAFF || role == Role.ADMIN;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** SessionRegistry 가 같은 회원의 세션을 찾을 수 있도록 ID 기준 비교 */
    @Override
    public boolean equals(Object o) {
        return o instanceof UserPrincipal other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
