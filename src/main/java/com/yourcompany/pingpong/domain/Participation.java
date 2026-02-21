package com.yourcompany.pingpong.domain;

import jakarta.persistence.*; // 또는 javax.persistence.*
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore; // ⭐⭐ 이 임포트 추가! ⭐⭐

@Entity
@Getter
@Setter
@Table(name = "participations")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;
    private String status;

    // ⭐ 시드 번호 추가 (조 편성 시 사용)
    private Integer seedNumber;
    private LocalDateTime registeredAt;

    // ⭐⭐ 아래 네 개의 ManyToOne 필드 위에 @JsonIgnore 추가! ⭐⭐
    @JsonIgnore // ⭐ 추가 ⭐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @JsonIgnore // ⭐ 추가 ⭐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore // ⭐ 추가 ⭐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @JsonIgnore // ⭐ 추가 ⭐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_group_id")
    private MatchGroup matchGroup;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자, toString 등...
    public Participation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 혹시 매핑을 위한 생성자가 있다면...
}