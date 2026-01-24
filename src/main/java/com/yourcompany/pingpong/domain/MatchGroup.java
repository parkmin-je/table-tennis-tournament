package com.yourcompany.pingpong.domain;

import jakarta.persistence.*; // 또는 javax.persistence.*

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList; // List를 위해
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "match_groups") // ⭐⭐⭐ 이거다, 이 씹새끼가 문제였다! ⭐⭐⭐
public class MatchGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 예: "A조", "B조"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @OneToMany(mappedBy = "matchGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "matchGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Lombok @NoArgsConstructor 추가
    public MatchGroup() {}

    // 생성자 (필요한 경우)
    public MatchGroup(String name, Tournament tournament) {
        this.name = name;
        this.tournament = tournament;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 편의 메서드 (연관 관계 설정을 위해)
    public void addParticipation(Participation participation) {
        this.participations.add(participation);
        participation.setMatchGroup(this);
    }

    public void addMatch(Match match) {
        this.matches.add(match);
        match.setMatchGroup(this);
    }
}