package com.parkminje.pingpong.home;

import com.parkminje.pingpong.board.Post;
import com.parkminje.pingpong.board.PostService;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentRepository;
import com.parkminje.pingpong.tournament.TournamentService;
import com.parkminje.pingpong.tournament.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    public record Champion(Tournament tournament, String name, String club) {
    }

    public record HomeView(List<Tournament> live, List<Tournament> upcoming, List<com.parkminje.pingpong.live.TournamentBoard> liveBoards,
                           List<Player> topPlayers, List<Post> notices, List<Champion> champions) {
    }

    private final TournamentService tournamentService;
    private final TournamentRepository tournamentRepository;
    private final com.parkminje.pingpong.live.TournamentBoardService boardService;
    private final PlayerService playerService;
    private final PostService postService;

    public HomeView load() {
        List<Tournament> featured = tournamentService.featured();
        List<Champion> champions = tournamentRepository.findAllByOrderByStartAtDesc().stream()
                .filter(t -> t.getStatus() == TournamentStatus.COMPLETED && t.getChampion() != null)
                .limit(3)
                .map(t -> new Champion(t, t.getChampion().getDisplayName(), t.getChampion().getPlayer().getClubName()))
                .toList();
        return new HomeView(
                featured.stream().filter(t -> t.getStatus() == TournamentStatus.IN_PROGRESS).toList(),
                featured.stream().filter(t -> t.getStatus() != TournamentStatus.IN_PROGRESS).limit(6).toList(),
                featured.stream().filter(t -> t.getStatus() == TournamentStatus.IN_PROGRESS)
                        .map(t -> boardService.load(t.getId())).toList(),
                playerService.topRanked(5),
                postService.latestNotices(),
                champions);
    }
}
