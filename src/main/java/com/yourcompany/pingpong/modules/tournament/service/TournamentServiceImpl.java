package com.yourcompany.pingpong.modules.tournament.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.tournament.dto.TournamentDto;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토너먼트 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;

    @Override
    public List<Tournament> getAllTournaments() {
        log.info("[TournamentService] 전체 대회 목록 조회");
        return tournamentRepository.findAllWithParticipations();
    }

    @Override
    public List<Tournament> getAllTournamentsForAdmin() {
        log.info("[TournamentService] 관리자용 대회 목록 조회");
        return tournamentRepository.findAllWithCreator();
    }

    @Override
    public Tournament getTournamentById(Long id) {
        log.debug("[TournamentService] 대회 조회 - ID: {}", id);
        return tournamentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[TournamentService] 대회를 찾을 수 없음 - ID: {}", id);
                    return new IllegalArgumentException("대회를 찾을 수 없습니다. ID=" + id);
                });
    }

    @Override
    public Tournament getTournamentByIdWithDetails(Long id) {
        log.debug("[TournamentService] 대회 상세 조회 - ID: {}", id);
        return tournamentRepository.findByIdWithParticipations(id)
                .orElseThrow(() -> {
                    log.error("[TournamentService] 대회를 찾을 수 없음 - ID: {}", id);
                    return new IllegalArgumentException("대회를 찾을 수 없습니다. ID=" + id);
                });
    }

    @Override
    @Transactional
    public Tournament createTournament(TournamentDto dto, User creator) {
        log.info("[TournamentService] 대회 생성 시작 - 제목: {}", dto.getTitle());

        Tournament tournament = Tournament.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .type(dto.getType())
                .status(dto.getStatus() != null ? dto.getStatus() : TournamentStatus.READY)
                .name(dto.getName())
                .creator(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Tournament savedTournament = tournamentRepository.save(tournament);
        log.info("[TournamentService] ✅ 대회 생성 완료 - ID: {}, 제목: {}", 
                savedTournament.getId(), savedTournament.getTitle());

        return savedTournament;
    }

    @Override
    @Transactional
    public Tournament updateTournament(Long id, TournamentDto dto) {
        log.info("[TournamentService] 대회 수정 시작 - ID: {}", id);

        Tournament tournament = getTournamentById(id);

        tournament.setTitle(dto.getTitle());
        tournament.setDescription(dto.getDescription());
        tournament.setStartDate(dto.getStartDate());
        tournament.setEndDate(dto.getEndDate());
        tournament.setType(dto.getType());
        tournament.setStatus(dto.getStatus());
        tournament.setName(dto.getName());
        tournament.setUpdatedAt(LocalDateTime.now());

        Tournament updatedTournament = tournamentRepository.save(tournament);
        log.info("[TournamentService] ✅ 대회 수정 완료 - ID: {}", updatedTournament.getId());

        return updatedTournament;
    }

    @Override
    @Transactional
    public void deleteTournament(Long id) {
        log.info("[TournamentService] 대회 삭제 시작 - ID: {}", id);

        Tournament tournament = getTournamentById(id);
        tournamentRepository.delete(tournament);

        log.info("[TournamentService] ✅ 대회 삭제 완료 - ID: {}", id);
    }

    @Override
    public List<Tournament> getOngoingTournaments() {
        log.info("[TournamentService] 진행 중인 대회 조회");
        List<TournamentStatus> ongoingStatuses = Arrays.asList(
                TournamentStatus.RECRUITING,
                TournamentStatus.IN_PROGRESS,
                TournamentStatus.MAIN_READY
        );
        List<Tournament> tournaments = tournamentRepository.findOngoingTournamentsWithParticipations(ongoingStatuses);
        log.debug("[TournamentService] 진행 중인 대회 {}개 발견", tournaments.size());
        return tournaments;
    }
}
