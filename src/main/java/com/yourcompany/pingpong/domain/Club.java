package com.yourcompany.pingpong.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore; // ⭐⭐ 이 임포트 추가! ⭐⭐
import java.time.LocalDateTime;
import java.util.List; // players 컬렉션을 위해

@Entity
@Getter
@Setter
@Table(name = "clubs") // 테이블명이 'club' 이 아닌 'clubs' 라면 이것도 중요!
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String region;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ⭐⭐ 이것이 문제의 근원! Club 엔티티 안에 Players 컬렉션이 지연 로딩되어 있을 거야! ⭐⭐
    @JsonIgnore // ⭐⭐⭐ 이 한 줄을 추가해 줘! ⭐⭐⭐
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players; // 이 필드명은 'players' 말고 다른 이름일 수도 있어!

    public Club() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}