package com.parkminje.pingpong.match;

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
@Table(name = "match_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MatchGroup(Tournament tournament, String name, int ordinal) {
        this.tournament = tournament;
        this.name = name;
        this.ordinal = ordinal;
        this.createdAt = LocalDateTime.now();
    }
}
