package com.parkminje.pingpong.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);

    @Query("""
            select u from User u
            where :q is null or :q = '' or u.username like concat('%', :q, '%')
               or u.name like concat('%', :q, '%') or u.email like concat('%', :q, '%')
            order by u.id desc
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
