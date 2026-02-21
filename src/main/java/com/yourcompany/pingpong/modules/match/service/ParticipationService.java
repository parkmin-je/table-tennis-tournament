package com.yourcompany.pingpong.modules.match.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yourcompany.pingpong.modules.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcompany.pingpong.domain.Participation;
import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.match.repository.ParticipationRepository;
import com.yourcompany.pingpong.modules.player.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final PlayerRepository playerRepository;
    private final NotificationService notificationService;

    /**
     * ⭐⭐⭐ 수정: 로그인한 사용자가 바로 참가 신청 (Player 자동 생성/연결) ⭐⭐⭐
     */
    @Transactional
    public Participation applyParticipation(User user, Tournament tournament) {

        if (participationRepository.existsByTournamentAndUser(tournament, user)) {
            throw new IllegalStateException("이미 이 대회에 참가 신청하셨습니다.");
        }

        // ⭐⭐⭐ Player 생성 또는 조회 ⭐⭐⭐
        Player player = playerRepository.findByUserUsername(user.getUsername())
                .orElseGet(() -> {
                    log.info("Creating new Player for user: {}", user.getUsername());
                    Player newPlayer = Player.builder()
                            .name(user.getName() != null ? user.getName() : user.getUsername())
                            .ranking(1000) // ⭐ rank → ranking으로 변경
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return playerRepository.save(newPlayer);
                });

        log.info("Player found/created: ID={}, Name={}", player.getId(), player.getName());

        // Participation 생성 및 Player 연결
        Participation p = new Participation();
        p.setUser(user);
        p.setPlayer(player);  // ⭐⭐⭐ Player 설정! ⭐⭐⭐
        p.setTournament(tournament);
        p.setTeamName(user.getUsername() + "팀");
        p.setRegisteredAt(LocalDateTime.now());
        p.setStatus("신청완료");

        Participation saved = participationRepository.save(p);

        // ⭐ 알림 생성
        notificationService.createNotification(
                user,
                "참가 신청 완료",
                tournament.getTitle() + " 대회 참가 신청이 완료되었습니다.",
                "SUCCESS",
                tournament
        );

        log.info("PARTICIPATION: User {} (Player ID: {}) applied to tournament {}",
                user.getUsername(), player.getId(), tournament.getId());
        return saved;
    }

    /**
     * ⭐⭐⭐ 전체 참가자 조회 (User, Tournament JOIN FETCH) ⭐⭐⭐
     */
    public List<Participation> findAll() {
        return participationRepository.findAllWithUserAndTournament();
    }

    /**
     * username으로 참가자 조회
     */
    public List<Participation> findByUsername(String username) {
        return participationRepository.findByUserUsername(username);
    }

    /**
     * 참가 상태 업데이트
     */
    @Transactional
    public void updateStatus(Long participationId, String newStatus) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 참가신청을 찾을 수 없습니다. ID=" + participationId));

        String oldStatus = participation.getStatus();
        participation.setStatus(newStatus);
        participationRepository.save(participation);

        // ⭐ 상태 변경 알림
        notificationService.createNotification(
                participation.getUser(),
                "참가 신청 상태 변경",
                participation.getTournament().getTitle() + " 대회 참가 상태가 " + newStatus + "로 변경되었습니다.",
                "INFO",
                participation.getTournament()
        );

        log.info("[관리자] 참가신청 상태 변경 완료 → ID: {}, 상태: {} → {}", participationId, oldStatus, newStatus);
    }

    /**
     * 대회별 참가자 조회
     */
    public List<Participation> findByTournament(Tournament tournament) {
        return participationRepository.findByTournament(tournament);
    }

    /**
     * 상태별 통계 계산
     */
    public Map<String, Long> getStatusStats() {
        List<Participation> list = participationRepository.findAllWithUserAndTournament();
        return list.stream()
                .collect(Collectors.groupingBy(Participation::getStatus, Collectors.counting()));
    }

    /**
     * 참가 취소
     */
    @Transactional
    public void cancelParticipation(Long participationId, User user) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("참가 신청을 찾을 수 없습니다."));

        if (!participation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 참가 신청만 취소할 수 있습니다.");
        }

        if ("승인완료".equals(participation.getStatus()) || "입금완료".equals(participation.getStatus())) {
            throw new IllegalStateException("이미 승인된 참가 신청은 관리자에게 문의하세요.");
        }

        participationRepository.delete(participation);
        log.info("CANCEL: Participation {} deleted successfully", participationId);
    }
}