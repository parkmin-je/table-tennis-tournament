package com.parkminje.pingpong.entry;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.ForbiddenException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.live.LiveEvent;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.match.MatchService;
import com.parkminje.pingpong.notification.Notification;
import com.parkminje.pingpong.notification.NotificationService;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerRepository;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentRepository;
import com.parkminje.pingpong.tournament.TournamentStatus;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EntryService {

    private static final List<EntryStatus> ACTIVE = List.of(EntryStatus.APPLIED, EntryStatus.CONFIRMED);

    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;

    /** 회원 본인 참가 신청 */
    public Entry apply(Long tournamentId, User user, Long partnerId) {
        Tournament t = lock(tournamentId);
        if (!t.isRegistrationOpen(LocalDateTime.now())) {
            throw new BusinessException("참가 신청 기간이 아닙니다.");
        }
        Player me = playerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("먼저 내 선수 프로필(성별·부수·클럽)을 등록하세요."));
        Player partner = resolvePartner(t, partnerId, me);
        validate(t, me, partner, false);
        EntryStatus status = t.isAutoConfirm() ? EntryStatus.CONFIRMED : EntryStatus.APPLIED;
        Entry entry = save(new Entry(t, me, partner, user, status));
        notificationService.notifyEntry(entry, Notification.Type.SUCCESS, "참가 신청 완료",
                t.getTitle() + " 참가 신청이 접수되었습니다." + (status == EntryStatus.APPLIED ? " 운영진 확인 후 확정됩니다." : ""),
                "/tournament/" + t.getId());
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
        return entry;
    }

    /** 운영진 현장/대리 접수 (접수 기간과 무관, 대진 생성 전까지) */
    public Entry register(Long tournamentId, Long playerId, Long partnerId, User staff, boolean ignoreRestriction) {
        Tournament t = lock(tournamentId);
        if (t.getStatus().isFinished() || matchRepository.countByTournamentId(t.getId()) > 0) {
            throw new BusinessException("대진이 만들어진 뒤에는 엔트리를 추가할 수 없습니다. 대진을 초기화한 뒤 추가하세요.");
        }
        Player player = playerRepository.findById(playerId).orElseThrow(() -> NotFoundException.of("선수", playerId));
        Player partner = resolvePartner(t, partnerId, player);
        validate(t, player, partner, ignoreRestriction);
        Entry entry = save(new Entry(t, player, partner, staff, EntryStatus.CONFIRMED));
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
        return entry;
    }

    /** 회원 본인 신청 취소 (대진 생성 전까지) */
    public void cancelByUser(Long entryId, User user) {
        Entry entry = find(entryId);
        Tournament t = lock(entry.getTournament().getId());
        if (!entry.belongsTo(user)) {
            throw new ForbiddenException("본인의 신청만 취소할 수 있습니다.");
        }
        if (matchRepository.countByTournamentId(t.getId()) > 0) {
            throw new BusinessException("대진이 발표된 뒤에는 운영진에게 기권을 요청하세요.");
        }
        if (!entry.getStatus().isActive()) {
            throw new BusinessException("이미 취소되었거나 반려된 신청입니다.");
        }
        entry.changeStatus(EntryStatus.WITHDRAWN);
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
    }

    /** 운영진 상태 변경 (신청 ↔ 확정 ↔ 반려). 기권은 withdraw 사용 */
    public void changeStatus(Long entryId, EntryStatus status) {
        Entry entry = find(entryId);
        Tournament t = lock(entry.getTournament().getId());
        if (status == EntryStatus.WITHDRAWN) {
            withdraw(entryId);
            return;
        }
        boolean drawn = matchRepository.countByTournamentId(t.getId()) > 0;
        if (drawn) {
            throw new BusinessException("대진 생성 후에는 확정/반려를 바꿀 수 없습니다. 필요하면 기권 처리하세요.");
        }
        if (status.isActive() && !entry.getStatus().isActive()) {
            if (entryRepository.isPlayerEntered(t.getId(), entry.getPlayer().getId())
                    || (entry.getPartner() != null && entryRepository.isPlayerEntered(t.getId(), entry.getPartner().getId()))) {
                throw new BusinessException("같은 선수가 이미 다른 엔트리로 참가 중입니다.");
            }
            checkCapacity(t);
        }
        EntryStatus before = entry.getStatus();
        entry.changeStatus(status);
        if (before != status && (status == EntryStatus.CONFIRMED || status == EntryStatus.REJECTED)) {
            notificationService.notifyEntry(entry, status == EntryStatus.CONFIRMED ? Notification.Type.SUCCESS : Notification.Type.WARNING,
                    "참가 " + status.getLabel(), t.getTitle() + " 참가 신청이 " + status.getLabel() + "되었습니다.", "/tournament/" + t.getId());
        }
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
    }

    /** 신청 상태 엔트리 일괄 확정 */
    public int confirmAll(Long tournamentId) {
        Tournament t = lock(tournamentId);
        if (matchRepository.countByTournamentId(t.getId()) > 0) {
            throw new BusinessException("대진 생성 후에는 일괄 확정할 수 없습니다.");
        }
        List<Entry> applied = entryRepository.findByTournamentIdAndStatusInOrderByIdAsc(t.getId(), List.of(EntryStatus.APPLIED));
        applied.forEach(e -> {
            e.changeStatus(EntryStatus.CONFIRMED);
            notificationService.notifyEntry(e, Notification.Type.SUCCESS, "참가 확정",
                    t.getTitle() + " 참가가 확정되었습니다.", "/tournament/" + t.getId());
        });
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
        return applied.size();
    }

    /** 기권 처리. 대회 진행 중이면 남은 경기는 상대 부전승 */
    public void withdraw(Long entryId) {
        Entry entry = find(entryId);
        Tournament t = lock(entry.getTournament().getId());
        if (entry.isWithdrawn()) {
            return;
        }
        if (t.getStatus().isFinished()) {
            throw new BusinessException("종료된 대회입니다.");
        }
        entry.changeStatus(EntryStatus.WITHDRAWN);
        if (t.getStatus() == TournamentStatus.IN_PROGRESS) {
            matchService.applyWithdrawal(entry);
        } else {
            events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.ENTRY_CHANGED));
        }
        notificationService.notifyEntry(entry, Notification.Type.WARNING, "기권 처리",
                t.getTitle() + " 기권 처리되었습니다.", "/tournament/" + t.getId());
        log.info("기권: entry={} tournament={}", entry.getId(), t.getId());
    }

    public void setCheckIn(Long entryId, boolean checkedIn) {
        Entry entry = find(entryId);
        entry.setCheckedIn(checkedIn);
        events.publishEvent(LiveEvent.of(entry.getTournament().getId(), LiveEvent.Type.ENTRY_CHANGED));
    }

    public void setPaid(Long entryId, boolean paid) {
        find(entryId).setPaid(paid);
    }

    public void setSeedAndMemo(Long entryId, Integer seed, String memo) {
        Entry entry = find(entryId);
        if (seed != null && seed < 1) {
            throw new BusinessException("시드는 1 이상이어야 합니다.");
        }
        if (seed != null && matchRepository.countByTournamentId(entry.getTournament().getId()) > 0
                && !seed.equals(entry.getSeed())) {
            throw new BusinessException("대진 생성 후에는 시드를 바꿀 수 없습니다.");
        }
        entry.setSeed(seed);
        entry.setMemo(memo == null || memo.isBlank() ? null : memo.trim());
    }

    @Transactional(readOnly = true)
    public List<Entry> findForTournament(Long tournamentId) {
        return entryRepository.findByTournamentIdOrderByIdAsc(tournamentId);
    }

    @Transactional(readOnly = true)
    public Entry find(Long entryId) {
        return entryRepository.findById(entryId).orElseThrow(() -> NotFoundException.of("엔트리", entryId));
    }

    // ───────────────────────────────────────────────────────

    private Player resolvePartner(Tournament t, Long partnerId, Player me) {
        if (!t.isDoubles()) {
            if (partnerId != null) {
                throw new BusinessException("단식 대회에는 파트너를 지정할 수 없습니다.");
            }
            return null;
        }
        if (partnerId == null) {
            throw new BusinessException("복식 대회는 파트너를 선택해야 합니다.");
        }
        if (partnerId.equals(me.getId())) {
            throw new BusinessException("본인을 파트너로 지정할 수 없습니다.");
        }
        return playerRepository.findById(partnerId).orElseThrow(() -> NotFoundException.of("파트너 선수", partnerId));
    }

    private void validate(Tournament t, Player player, Player partner, boolean ignoreRestriction) {
        if (t.getStatus().isFinished()) {
            throw new BusinessException("종료되었거나 취소된 대회입니다.");
        }
        for (Player p : partner == null ? List.of(player) : List.of(player, partner)) {
            if (entryRepository.isPlayerEntered(t.getId(), p.getId())) {
                throw new BusinessException(p.getName() + " 선수는 이미 이 대회에 신청되어 있습니다.");
            }
            if (!ignoreRestriction) {
                if (!t.getGenderRestriction().allows(p.getGender())) {
                    throw new BusinessException(p.getName() + " 선수는 " + t.getGenderRestriction().getLabel() + " 참가 대상이 아닙니다.");
                }
                if (!t.acceptsDivision(p.getDivision())) {
                    throw new BusinessException(p.getName() + " 선수(" + p.getDivisionLabel() + ")는 참가 부수(" + t.getDivisionLabel() + ") 대상이 아닙니다.");
                }
            }
        }
        checkCapacity(t);
    }

    private void checkCapacity(Tournament t) {
        if (t.getMaxEntries() != null && entryRepository.countByTournamentIdAndStatusIn(t.getId(), ACTIVE) >= t.getMaxEntries()) {
            throw new BusinessException("정원(" + t.getMaxEntries() + "팀)이 모두 찼습니다.");
        }
    }

    /**
     * (tournament, player) 유니크 제약이 있으므로, 같은 선수가 예전에 취소·반려된 엔트리가 있으면 그것을 되살린다.
     */
    private Entry save(Entry entry) {
        var previous = entryRepository.findByTournamentIdAndPlayerId(entry.getTournament().getId(), entry.getPlayer().getId());
        if (previous.isPresent()) {
            Entry old = previous.get();
            old.reactivate(entry.getPartner(), entry.getApplicant(), entry.getStatus());
            return old;
        }
        try {
            return entryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("이미 신청 내역이 있습니다. 잠시 후 다시 확인해 주세요.");
        }
    }

    private Tournament lock(Long id) {
        return tournamentRepository.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("대회", id));
    }
}
