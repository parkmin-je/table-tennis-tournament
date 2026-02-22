package com.yourcompany.pingpong.modules.player.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.modules.match.repository.MatchRepository;
import com.yourcompany.pingpong.modules.player.dto.PlayerStatsDto;
import com.yourcompany.pingpong.modules.player.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    /**
     * 전체 선수 목록 조회 (클럽 정보 포함)
     */
    public List<Player> findAllWithClub() {
        log.info("SERVICE INFO: Fetching all players with club information");
        List<Player> players = playerRepository.findAllWithClub();
        log.debug("SERVICE DEBUG: Found {} players with club", players.size());
        return players;
    }

    /**
     * 단일 선수 조회
     */
    public Player getPlayer(Long id) {
        log.debug("SERVICE DEBUG: Attempting to get player with ID: {}", id);
        return playerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("SERVICE ERROR: Player not found for ID: {}", id);
                    return new IllegalArgumentException("선수를 찾을 수 없습니다. ID=" + id);
                });
    }

    /**
     * ⭐ 단일 선수 조회 (Club 포함) - detail 페이지용 ⭐
     */
    public Player getPlayerWithClub(Long id) {
        log.debug("SERVICE DEBUG: Attempting to get player with club for ID: {}", id);
        return playerRepository.findByIdWithClub(id)
                .orElseThrow(() -> {
                    log.error("SERVICE ERROR: Player not found for ID: {}", id);
                    return new IllegalArgumentException("선수를 찾을 수 없습니다. ID=" + id);
                });
    }

    /**
     * 선수 등록
     */
    @Transactional
    public Player save(Player player) {
        log.info("SERVICE INFO: Saving new player: {}", player.getName());
        return playerRepository.save(player);
    }

    /**
     * 선수 수정
     */
    @Transactional
    public Player update(Long id, Player updatedPlayer) {
        log.info("SERVICE INFO: Updating player with ID: {}", id);
        Player existing = playerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("SERVICE ERROR: Player not found for update (ID: {})", id);
                    return new IllegalArgumentException("수정할 선수를 찾을 수 없습니다. ID=" + id);
                });

        existing.setName(updatedPlayer.getName());
        existing.setAge(updatedPlayer.getAge());
        existing.setGender(updatedPlayer.getGender());
        existing.setRanking(updatedPlayer.getRanking());
        existing.setClub(updatedPlayer.getClub());
        existing.setPosition(updatedPlayer.getPosition());
        existing.setContactInfo(updatedPlayer.getContactInfo());
        existing.setUpdatedAt(LocalDateTime.now());

        return playerRepository.save(existing);
    }

    /**
     * 선수 삭제
     */
    @Transactional
    public void delete(Long id) {
        log.info("SERVICE INFO: Deleting player with ID: {}", id);
        if (!playerRepository.existsById(id)) {
            log.error("SERVICE ERROR: Player not found for deletion (ID: {})", id);
            throw new IllegalArgumentException("삭제할 선수를 찾을 수 없습니다. ID=" + id);
        }
        playerRepository.deleteById(id);
        log.info("SERVICE INFO: Successfully deleted player with ID: {}", id);
    }

    /**
     * ⭐ 전체 선수 수 조회
     */
    public long countAllPlayers() {
        return playerRepository.count();
    }

    /**
     * ⭐ TOP 선수 조회 (랭킹순)
     */
    public List<Player> getTopPlayers(int limit) {
        return playerRepository.findAllWithClub().stream()
                .filter(p -> p.getRanking() != null)
                .sorted((p1, p2) -> p1.getRanking().compareTo(p2.getRanking()))
                .limit(limit)
                .toList();
    }

    /**
     * ⭐ 클럽별 선수 목록 조회 (클럽 상세 페이지용)
     */
    public List<Player> findByClubId(Long clubId) {
        log.info("SERVICE INFO: Finding players by club ID: {}", clubId);
        return playerRepository.findByClubId(clubId);
    }

    /**
     * ⭐ 선수 전적 통계 조회
     */
    public PlayerStatsDto getPlayerStats(Long playerId) {
        Player player = playerRepository.findByIdWithClub(playerId)
                .orElseThrow(() -> new IllegalArgumentException("선수를 찾을 수 없습니다. ID=" + playerId));
        List<Match> completed = matchRepository.findCompletedMatchesByPlayerId(playerId);
        log.info("SERVICE INFO: Stats for player {} - {} completed matches", player.getName(), completed.size());
        return new PlayerStatsDto(player, completed);
    }
}