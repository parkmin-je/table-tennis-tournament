package com.parkminje.pingpong.player;

import com.parkminje.pingpong.tournament.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "rating_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(name = "before_rating", nullable = false)
    private int beforeRating;

    @Column(name = "after_rating", nullable = false)
    private int afterRating;

    @Column(name = "match_count", nullable = false)
    private int matchCount;

    @Column(nullable = false)
    private int wins;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RatingHistory(Player player, Tournament tournament, int beforeRating, int afterRating, int matchCount, int wins) {
        this.player = player;
        this.tournament = tournament;
        this.beforeRating = beforeRating;
        this.afterRating = afterRating;
        this.matchCount = matchCount;
        this.wins = wins;
        this.createdAt = LocalDateTime.now();
    }

    public int getDelta() {
        return afterRating - beforeRating;
    }
}
