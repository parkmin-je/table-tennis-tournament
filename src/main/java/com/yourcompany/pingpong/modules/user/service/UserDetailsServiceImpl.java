package com.yourcompany.pingpong.modules.user.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Primary
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 이 줄이 있어야 해!
        System.out.println("DEBUG: UserDetailsServiceImpl.loadUserByUsername 호출됨. username: " + username);

        Optional<User> _user = this.userRepository.findByUsername(username);
        if (_user.isEmpty()) {
            // 이 줄이 있어야 해!
            System.out.println("DEBUG: 사용자를 찾을 수 없습니다: " + username);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
        User user = _user.get();
        // 이 줄이 있어야 해! DB에서 무엇을 가져오는지 정확히!
        System.out.println("DEBUG: DB에서 가져온 사용자 role: '" + user.getRole() + "'"); // 작은따옴표 추가해서 공백 여부도 확인!

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        // 이 줄이 있어야 해! 어떤 권한이 최종적으로 부여되는지!
        System.out.println("DEBUG: 부여될 최종 권한 목록: " + authorities);

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }
}