package com.parkminje.pingpong.tournament;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.EntryRepository;
import com.parkminje.pingpong.live.LiveEvent;
import com.parkminje.pingpong.match.MatchGroupRepository;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchGroupRepository groupRepository;
    private final EntryRepository entryRepository;
    private final ApplicationEventPublisher events;

    public Tournament create(TournamentSpec spec, User creator) {
        validate(spec);
        return tournamentRepository.save(new Tournament(spec, creator));
    }

    public Tournament update(Long id, TournamentSpec spec) {
        validate(spec);
        Tournament t = lock(id);
        if (t.getStatus().isFinished()) {
            throw new BusinessException("종료되었거나 취소된 대회는 수정할 수 없습니다.");
        }
        boolean drawn = matchRepository.countByTournamentId(id) > 0;
        if (drawn && t.changesStructure(spec)) {
            throw new BusinessException("대진 생성 후에는 경기 방식·게임 수·조 설정을 바꿀 수 없습니다. 대진을 초기화한 뒤 수정하세요.");
        }
        if (spec.maxEntries() != null && spec.maxEntries() < entryRepository.countByTournamentIdAndStatusIn(id,
                List.of(com.parkminje.pingpong.entry.EntryStatus.APPLIED, com.parkminje.pingpong.entry.EntryStatus.CONFIRMED))) {
            throw new BusinessException("정원을 현재 신청 인원보다 적게 줄일 수 없습니다.");
        }
        t.apply(spec);
        events.publishEvent(LiveEvent.of(id, LiveEvent.Type.TOURNAMENT_UPDATED));
        return t;
    }

    /** 상태 전환: 공개(접수 시작) / 접수 마감 / 접수 재개 / 취소 */
    public void transition(Long id, TournamentStatus target) {
        Tournament t = lock(id);
        TournamentStatus current = t.getStatus();
        boolean drawn = matchRepository.countByTournamentId(id) > 0;
        boolean allowed = switch (target) {
            case RECRUITING -> (current == TournamentStatus.DRAFT || current == TournamentStatus.CLOSED) && !drawn;
            case CLOSED -> current == TournamentStatus.RECRUITING;
            case DRAFT -> current == TournamentStatus.RECRUITING && !drawn;
            case CANCELLED -> !current.isFinished();
            case IN_PROGRESS, COMPLETED -> false; // 대진 생성/결승 종료로만 전환
        };
        if (!allowed) {
            throw new BusinessException("'" + current.getLabel() + "' 상태에서 '" + target.getLabel() + "'(으)로 바꿀 수 없습니다.");
        }
        t.changeStatus(target);
        events.publishEvent(LiveEvent.of(id, LiveEvent.Type.TOURNAMENT_UPDATED));
        log.info("대회 상태 변경: tournament={} {} -> {}", id, current, target);
    }

    /** 대진이 없는 대회만 삭제 (진행 기록이 있으면 취소 처리를 권장) */
    public void delete(Long id) {
        Tournament t = lock(id);
        if (matchRepository.countByTournamentId(id) > 0) {
            throw new BusinessException("경기 기록이 있는 대회는 삭제할 수 없습니다. 대진을 초기화하거나 '취소' 상태로 바꾸세요.");
        }
        entryRepository.deleteByTournamentId(id);
        groupRepository.deleteByTournamentId(id);
        tournamentRepository.delete(t);
    }

    @Transactional(readOnly = true)
    public Tournament get(Long id) {
        return tournamentRepository.findById(id).orElseThrow(() -> NotFoundException.of("대회", id));
    }

    @Transactional(readOnly = true)
    public Page<Tournament> searchPublic(TournamentStatus status, String q, int page) {
        return tournamentRepository.searchPublic(status, blankToNull(q), PageRequest.of(Math.max(page, 0), 12));
    }

    @Transactional(readOnly = true)
    public List<Tournament> allForAdmin() {
        return tournamentRepository.findAllByOrderByStartAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Tournament> featured() {
        return tournamentRepository.findByStatusInOrderByStartAtAsc(
                Set.of(TournamentStatus.IN_PROGRESS, TournamentStatus.RECRUITING, TournamentStatus.CLOSED));
    }

    static void validate(TournamentSpec s) {
        if (s.title() == null || s.title().isBlank()) {
            throw new BusinessException("대회명을 입력하세요.");
        }
        if (s.startAt() == null || s.endAt() == null || s.endAt().isBefore(s.startAt())) {
            throw new BusinessException("종료 일시는 시작 일시 이후여야 합니다.");
        }
        if (s.registrationDeadline() != null && s.registrationDeadline().isAfter(s.startAt())) {
            throw new BusinessException("접수 마감은 대회 시작 전이어야 합니다.");
        }
        if (s.bestOf() < 1 || s.bestOf() > 9 || s.bestOf() % 2 == 0) {
            throw new BusinessException("게임 수는 1~9 사이 홀수여야 합니다.");
        }
        if (s.maxEntries() != null && s.maxEntries() < 2) {
            throw new BusinessException("정원은 2팀 이상이어야 합니다.");
        }
        if (s.tableCount() < 1 || s.tableCount() > 100) {
            throw new BusinessException("테이블 수는 1~100 사이여야 합니다.");
        }
        if (s.format() == TournamentFormat.GROUP_KNOCKOUT) {
            if (s.groupSize() < 3 || s.groupSize() > 8) {
                throw new BusinessException("조 인원은 3~8명이어야 합니다.");
            }
            if (s.advancePerGroup() < 1 || s.advancePerGroup() >= s.groupSize()) {
                throw new BusinessException("조별 본선 진출 인원은 1명 이상, 조 인원보다 적어야 합니다.");
            }
        }
        if (s.divisionMin() != null && s.divisionMax() != null && s.divisionMin() > s.divisionMax()) {
            throw new BusinessException("부수 범위가 올바르지 않습니다. (예: 3부~5부)");
        }
        if (s.entryFee() < 0) {
            throw new BusinessException("참가비는 0원 이상이어야 합니다.");
        }
    }

    private Tournament lock(Long id) {
        return tournamentRepository.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("대회", id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
