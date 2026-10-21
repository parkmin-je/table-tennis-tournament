package com.parkminje.pingpong.match;

import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.live.LiveEvent;
import com.parkminje.pingpong.notification.Notification;
import com.parkminje.pingpong.notification.NotificationService;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentRepository;
import com.parkminje.pingpong.tournament.TournamentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 경기 진행 (테이블 배정·결과 입력·수정·취소·기권 처리).
 * 모든 쓰기 작업은 대회 행 잠금(PESSIMISTIC_WRITE) 안에서 수행해 동시에 입력해도 상태가 꼬이지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchProgression progression;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;

    public Match start(Long matchId, int table) {
        Match m = loadLocked(matchId);
        Tournament t = m.getTournament();
        requireInProgress(t);
        if (!m.isScheduled()) {
            throw new BusinessException("대기 중인 경기만 시작할 수 있습니다. (현재: " + m.getStatus().getLabel() + ")");
        }
        if (!m.hasBothEntries()) {
            throw new BusinessException("아직 상대가 정해지지 않은 경기입니다.");
        }
        if (table < 1 || table > t.getTableCount()) {
            throw new BusinessException("테이블 번호는 1~" + t.getTableCount() + " 사이여야 합니다.");
        }
        List<Match> live = inProgress(t.getId());
        if (live.stream().anyMatch(x -> Integer.valueOf(table).equals(x.getTableNumber()))) {
            throw new BusinessException(table + "번 테이블에서 다른 경기가 진행 중입니다.");
        }
        for (Match x : live) {
            if (x.involves(m.getEntry1()) || x.involves(m.getEntry2())) {
                Entry busy = x.involves(m.getEntry1()) ? m.getEntry1() : m.getEntry2();
                throw new BusinessException(busy.getDisplayName() + " 선수가 " + x.getTableNumber() + "번 테이블에서 경기 중입니다.");
            }
        }
        m.start(table);
        callPlayers(m);
        events.publishEvent(LiveEvent.match(t.getId(), LiveEvent.Type.MATCH_STARTED, m.getId(),
                table + "번 테이블: " + m.getEntry1().getDisplayName() + " vs " + m.getEntry2().getDisplayName()));
        return m;
    }

    public void cancelStart(Long matchId) {
        Match m = loadLocked(matchId);
        if (!m.isInProgress()) {
            throw new BusinessException("진행 중인 경기가 아닙니다.");
        }
        m.cancelStart();
        events.publishEvent(LiveEvent.match(m.getTournament().getId(), LiveEvent.Type.MATCH_UPDATED, m.getId(), null));
    }

    /** 결과 입력 (진행 중 경기, 부전승은 대기 경기도 가능) */
    public Match record(Long matchId, ResultInput input) {
        Match m = loadLocked(matchId);
        Tournament t = m.getTournament();
        requireInProgress(t);
        if (m.isCompleted()) {
            throw new BusinessException("이미 결과가 입력된 경기입니다. 결과 수정을 사용하세요.");
        }
        if (!m.hasBothEntries()) {
            throw new BusinessException("아직 상대가 정해지지 않은 경기입니다.");
        }
        if (input.mode() != ResultInput.Mode.WALKOVER && !m.isInProgress()) {
            throw new BusinessException("시작(테이블 배정)한 경기만 결과를 입력할 수 있습니다.");
        }
        finish(m, input);
        progression.advance(m);
        announce(m, LiveEvent.Type.MATCH_COMPLETED);
        return m;
    }

    /** 끝난 경기의 결과 수정 (승자가 바뀌면 다음 경기 자리도 바뀐다) */
    public Match correct(Long matchId, ResultInput input) {
        Match m = loadLocked(matchId);
        requireEditable(m);
        if (m.getResultType() == ResultType.BYE) {
            throw new BusinessException("BYE 경기는 수정할 수 없습니다.");
        }
        GameScore score = input.toScore(m.getTournament().getBestOf());
        int newSide = input.resolveWinnerSide(score);
        boolean winnerChanges = newSide != m.winnerSide();
        if (winnerChanges) {
            progression.retract(m);
        }
        m.finish(m.entryAt(newSide), input.resultType(), score);
        if (winnerChanges) {
            progression.advance(m);
        }
        announce(m, LiveEvent.Type.MATCH_UPDATED);
        log.info("결과 수정: match={} winnerChanged={}", m.getId(), winnerChanges);
        return m;
    }

    /** 결과 취소 → 대기 상태로 되돌림 */
    public void reset(Long matchId) {
        Match m = loadLocked(matchId);
        requireEditable(m);
        if (m.getResultType() == ResultType.BYE) {
            throw new BusinessException("BYE 경기는 취소할 수 없습니다.");
        }
        progression.retract(m);
        m.clearResult();
        announce(m, LiveEvent.Type.MATCH_UPDATED);
    }

    /** 빈 테이블에 다음 순서 경기를 자동 배정. 배정한 경기 수 반환 */
    public int autoAssign(Long tournamentId) {
        Tournament t = tournamentRepository.findByIdForUpdate(tournamentId).orElseThrow(() -> NotFoundException.of("대회", tournamentId));
        requireInProgress(t);
        List<Match> all = matchRepository.findByTournamentIdOrderByPlayOrderAsc(t.getId());
        Set<Integer> usedTables = all.stream().filter(Match::isInProgress).map(Match::getTableNumber).collect(Collectors.toSet());
        TreeSet<Integer> free = IntStream.rangeClosed(1, t.getTableCount()).boxed()
                .filter(n -> !usedTables.contains(n)).collect(Collectors.toCollection(TreeSet::new));
        Set<Long> busy = new HashSet<>();
        all.stream().filter(Match::isInProgress).forEach(m -> {
            busy.add(m.getEntry1().getId());
            busy.add(m.getEntry2().getId());
        });

        int assigned = 0;
        for (Match m : all) {
            if (free.isEmpty()) {
                break;
            }
            if (!m.isScheduled() || !m.hasBothEntries()
                    || busy.contains(m.getEntry1().getId()) || busy.contains(m.getEntry2().getId())) {
                continue;
            }
            int table = free.pollFirst();
            m.start(table);
            busy.add(m.getEntry1().getId());
            busy.add(m.getEntry2().getId());
            callPlayers(m);
            assigned++;
        }
        if (assigned > 0) {
            events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.MATCH_STARTED));
        }
        return assigned;
    }

    /**
     * 대회 진행 중 엔트리가 기권하면 남은 경기를 모두 상대 부전승으로 처리한다.
     * (호출자가 대회 락을 잡고 엔트리 상태를 WITHDRAWN 으로 바꾼 뒤 호출)
     */
    public void applyWithdrawal(Entry entry) {
        List<Match> matches = matchRepository.findByTournamentIdOrderByPlayOrderAsc(entry.getTournament().getId());
        for (Match m : matches) {
            if (m.isCompleted() || !m.involves(entry)) {
                continue;
            }
            if (m.isInProgress()) {
                m.cancelStart();
            }
            progression.resolveAutomatic(m);
        }
        events.publishEvent(LiveEvent.of(entry.getTournament().getId(), LiveEvent.Type.ENTRY_CHANGED));
    }

    // ───────────────────────────────────────────────────────

    private void finish(Match m, ResultInput input) {
        GameScore score = input.toScore(m.getTournament().getBestOf());
        int side = input.resolveWinnerSide(score);
        m.finish(m.entryAt(side), input.resultType(), score);
    }

    private void requireEditable(Match m) {
        requireInProgressOrCompleted(m.getTournament());
        if (!m.isCompleted()) {
            throw new BusinessException("결과가 입력된 경기만 수정·취소할 수 있습니다.");
        }
        if (m.getStage() == MatchStage.GROUP
                && matchRepository.countByTournamentIdAndStage(m.getTournament().getId(), MatchStage.KNOCKOUT) > 0) {
            throw new BusinessException("본선 대진이 이미 만들어져 예선 결과를 바꿀 수 없습니다. 본선을 초기화한 뒤 수정하세요.");
        }
    }

    private void callPlayers(Match m) {
        Tournament t = m.getTournament();
        String title = "[경기 호출] " + m.getTableNumber() + "번 테이블";
        String link = "/tournament/" + t.getId();
        notificationService.notifyEntry(m.getEntry1(), Notification.Type.CALL, title,
                t.getTitle() + " " + m.getRoundLabel() + " vs " + m.getEntry2().getDisplayName() + " — " + m.getTableNumber() + "번 테이블로 와주세요.", link);
        notificationService.notifyEntry(m.getEntry2(), Notification.Type.CALL, title,
                t.getTitle() + " " + m.getRoundLabel() + " vs " + m.getEntry1().getDisplayName() + " — " + m.getTableNumber() + "번 테이블로 와주세요.", link);
    }

    private void announce(Match m, LiveEvent.Type type) {
        String msg = null;
        if (m.isCompleted() && m.getWinner() != null) {
            msg = m.getRoundLabel() + " " + m.getWinner().getDisplayName() + " 승 (" + m.getScoreLabel() + ")";
        }
        events.publishEvent(LiveEvent.match(m.getTournament().getId(), type, m.getId(), msg));
    }

    private List<Match> inProgress(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByPlayOrderAsc(tournamentId).stream().filter(Match::isInProgress).toList();
    }

    /** 경기가 속한 대회를 먼저 잠그고 경기를 다시 읽는다 */
    private Match loadLocked(Long matchId) {
        Long tournamentId = matchRepository.findTournamentIdById(matchId).orElseThrow(() -> NotFoundException.of("경기", matchId));
        tournamentRepository.findByIdForUpdate(tournamentId);
        return matchRepository.findWithDetailsById(matchId).orElseThrow(() -> NotFoundException.of("경기", matchId));
    }

    private static void requireInProgress(Tournament t) {
        if (t.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("진행중인 대회가 아닙니다. (현재: " + t.getStatus().getLabel() + ")");
        }
    }

    private static void requireInProgressOrCompleted(Tournament t) {
        if (t.getStatus() != TournamentStatus.IN_PROGRESS && t.getStatus() != TournamentStatus.COMPLETED) {
            throw new BusinessException("진행중이거나 종료된 대회의 경기만 수정할 수 있습니다.");
        }
    }
}
