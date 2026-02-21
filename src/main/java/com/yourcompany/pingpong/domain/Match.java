package com.yourcompany.pingpong.domain;

import jakarta.persistence.*; // ⭐⭐ LAZY 빨간줄 해결! ⭐⭐
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer matchNumber; 

    @Enumerated(EnumType.STRING) // ⭐⭐⭐ String 타입으로 DB에 저장되도록 설정! ⭐⭐⭐
    private MatchStatus status; // ⭐⭐⭐ MatchStatus Enum 사용! ⭐⭐⭐

    private LocalDateTime matchTime;
    private Integer score1;
    private Integer score2;
    private String winner;
    private String roundName;

    private Integer tableNumber; // ⭐⭐⭐ 탁구대 번호 추가! ⭐⭐⭐

    @JsonIgnore // JSON 직렬화 시 무시
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_group_id")
    private MatchGroup matchGroup;

    @JsonIgnore // JSON 직렬화 시 무시
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @JsonIgnore // JSON 직렬화 시 무시
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id")
    private Player player1;

    @JsonIgnore // JSON 직렬화 시 무시
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private Player player2;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ⭐⭐⭐ 기본 생성자 대신 @NoArgsConstructor와 @AllArgsConstructor를 사용하며, ⭐⭐⭐
    // ⭐⭐⭐ @PrePersist를 통해 필드 초기화를 담당하도록 변경! ⭐⭐⭐
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) { // ⭐⭐ 매치 생성 시 기본 상태를 SCHEDULED로! ⭐⭐
            this.status = MatchStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}