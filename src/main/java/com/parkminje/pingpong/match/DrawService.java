package com.parkminje.pingpong.match;

import com.parkminje.pingpong.bracket.GroupDrawer;
import com.parkminje.pingpong.bracket.KnockoutLayout;
import com.parkminje.pingpong.bracket.QualifierPlacement;
import com.parkminje.pingpong.bracket.RoundRobin;
import com.parkminje.pingpong.bracket.Standings;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryRepository;
import com.parkminje.pingpong.entry.EntryStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 대진 생성/초기화.
 * <ul>
 *   <li>토너먼트 방식: 확정 엔트리를 시드 순으로 배치한 본선 트리를 한 번에 만든다.</li>
 *   <li>리그+본선 방식: 먼저 조 편성과 리그 경기를 만들고, 리그가 모두 끝나면 본선 트리를 만든다.</li>
 * </ul>
 * 본선 트리는 처음부터 결승까지 모든 경기를 만들어 두고 다음 경기와 연결하므로,
 * 승자는 항상 정해진 자리로 올라가고 BYE 처리도 생성 시점에 끝난다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DrawService {

    private final TournamentRepository tournamentRepository;
    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final MatchGroupRepository groupRepository;
    private final StandingsService standingsService;
    private final MatchProgression progression;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;

    /** 대진 생성에 참여하는 엔트리 (확정 + 체크인 필수 대회면 체크인 완료) */
    @Transactional(readOnly = true)
    public List<Entry> drawableEntries(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> NotFoundException.of("대회", tournamentId));
        return eligible(t);
    }

    private List<Entry> eligible(Tournament t) {
        return entryRepository.findByTournamentIdAndStatusInOrderByIdAsc(t.getId(), List.of(EntryStatus.CONFIRMED)).stream()
                .filter(e -> !t.isRequireCheckIn() || e.isCheckedIn())
                .sorted(StandingsService.seedComparator())
                .toList();
    }

    /** 첫 대진 생성 (토너먼트: 본선 트리 / 리그+본선: 조 편성) */
    public void createInitialDraw(Long tournamentId) {
        Tournament t = lock(tournamentId);
        if (t.getStatus() != TournamentStatus.RECRUITING && t.getStatus() != TournamentStatus.CLOSED) {
            throw new BusinessException("접수중 또는 접수마감 상태에서만 대진을 만들 수 있습니다. (현재: " + t.getStatus().getLabel() + ")");
        }
        if (matchRepository.countByTournamentId(t.getId()) > 0) {
            throw new BusinessException("이미 대진이 생성되어 있습니다. 다시 만들려면 먼저 대진을 초기화하세요.");
        }
        List<Entry> entries = eligible(t);
        if (entries.size() < 2) {
            throw new BusinessException("대진을 만들 확정 엔트리가 2팀 이상 필요합니다. (현재 " + entries.size() + "팀"
                    + (t.isRequireCheckIn() ? ", 체크인 완료 기준" : "") + ")");
        }

        if (t.hasGroupStage()) {
            createGroups(t, entries);
        } else {
            buildKnockout(t, KnockoutLayout.placeBySeed(entries), 1);
        }
        t.changeStatus(TournamentStatus.IN_PROGRESS);
        entries.forEach(e -> notificationService.notifyEntry(e, Notification.Type.INFO, "대진 발표",
                t.getTitle() + " 대진이 발표되었습니다.", "/tournament/" + t.getId()));
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.DRAW_CREATED));
        log.info("대진 생성: tournament={} entries={} format={}", t.getId(), entries.size(), t.getFormat());
    }

    /** 리그 종료 후 본선 트리 생성 */
    public void createKnockoutFromGroups(Long tournamentId) {
        Tournament t = lock(tournamentId);
        if (!t.hasGroupStage()) {
            throw new BusinessException("예선 리그가 없는 대회입니다.");
        }
        if (t.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("진행중인 대회에서만 본선을 만들 수 있습니다.");
        }
        if (matchRepository.countByTournamentIdAndStage(t.getId(), MatchStage.KNOCKOUT) > 0) {
            throw new BusinessException("본선 대진이 이미 생성되었습니다.");
        }
        long unfinished = matchRepository.countByTournamentIdAndStageAndStatusNot(t.getId(), MatchStage.GROUP, MatchStatus.COMPLETED);
        if (unfinished > 0) {
            throw new BusinessException("예선 리그 경기가 " + unfinished + "경기 남아 있습니다.");
        }

        List<MatchGroup> groups = groupRepository.findByTournamentIdOrderByOrdinalAsc(t.getId());
        List<Entry> entries = entryRepository.findByTournamentIdOrderByIdAsc(t.getId());
        List<Match> matches = matchRepository.findByTournamentIdOrderByPlayOrderAsc(t.getId());
        List<StandingsService.GroupTable> tables = standingsService.compute(groups, entries, matches);

        List<QualifierPlacement.Qualifier<Entry>> qualifiers = new ArrayList<>();
        for (int g = 0; g < tables.size(); g++) {
            List<Standings.Row<Entry>> rows = tables.get(g).rows();
            int taken = 0;
            for (Standings.Row<Entry> row : rows) {
                if (taken >= t.getAdvancePerGroup()) {
                    break;
                }
                if (row.entry().isWithdrawn()) {
                    continue;
                }
                qualifiers.add(new QualifierPlacement.Qualifier<>(row.entry(), g, taken + 1));
                taken++;
            }
        }
        if (qualifiers.size() < 2) {
            throw new BusinessException("본선 진출자가 2팀 미만입니다.");
        }
        int startOrder = matches.stream().mapToInt(Match::getPlayOrder).max().orElse(0) + 1;
        buildKnockout(t, QualifierPlacement.place(qualifiers), startOrder);
        qualifiers.forEach(q -> notificationService.notifyEntry(q.entry(), Notification.Type.SUCCESS, "본선 진출",
                t.getTitle() + " 본선에 진출했습니다. (" + groups.get(q.groupIndex()).getName() + " " + q.place() + "위)",
                "/tournament/" + t.getId() + "/bracket"));
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.DRAW_CREATED));
        log.info("본선 생성: tournament={} qualifiers={}", t.getId(), qualifiers.size());
    }

    /** 아직 아무 경기도 치르지 않았을 때 대진 전체 초기화 */
    public void resetDraw(Long tournamentId) {
        Tournament t = lock(tournamentId);
        if (matchRepository.anyPlayed(t.getId(), MatchStage.GROUP) || matchRepository.anyPlayed(t.getId(), MatchStage.KNOCKOUT)) {
            throw new BusinessException("이미 시작되었거나 결과가 입력된 경기가 있어 대진을 초기화할 수 없습니다.");
        }
        deleteStage(t.getId(), MatchStage.KNOCKOUT);
        deleteStage(t.getId(), MatchStage.GROUP);
        entryRepository.findByTournamentIdOrderByIdAsc(t.getId()).forEach(e -> e.assignGroup(null));
        groupRepository.deleteByTournamentId(t.getId());
        t.changeStatus(TournamentStatus.CLOSED);
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.DRAW_RESET));
    }

    /** 리그+본선 대회에서 본선만 다시 뽑기 (본선 경기를 하나도 치르지 않았을 때) */
    public void resetKnockout(Long tournamentId) {
        Tournament t = lock(tournamentId);
        if (!t.hasGroupStage()) {
            throw new BusinessException("토너먼트 방식은 전체 대진 초기화를 사용하세요.");
        }
        if (matchRepository.anyPlayed(t.getId(), MatchStage.KNOCKOUT)) {
            throw new BusinessException("이미 진행된 본선 경기가 있어 초기화할 수 없습니다.");
        }
        deleteStage(t.getId(), MatchStage.KNOCKOUT);
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.DRAW_RESET));
    }

    // ───────────────────────────────────────────────────────

    private void createGroups(Tournament t, List<Entry> entries) {
        int groupCount = GroupDrawer.groupCount(entries.size(), t.getGroupSize());
        List<List<Entry>> drawn = GroupDrawer.draw(entries, groupCount, this::clubKey);

        List<Match> all = new ArrayList<>();
        List<List<RoundRobin.Pairing>> schedules = new ArrayList<>();
        List<MatchGroup> groups = new ArrayList<>();
        for (int g = 0; g < drawn.size(); g++) {
            MatchGroup group = groupRepository.save(new MatchGroup(t, GroupDrawer.groupName(g), g));
            groups.add(group);
            drawn.get(g).forEach(e -> e.assignGroup(group));
            schedules.add(RoundRobin.schedule(drawn.get(g).size()));
        }

        // 라운드 우선으로 섞어 모든 조가 고르게 진행되도록 순서 부여
        int maxRound = schedules.stream().flatMap(List::stream).mapToInt(RoundRobin.Pairing::round).max().orElse(0);
        int order = 1;
        for (int round = 1; round <= maxRound; round++) {
            for (int g = 0; g < groups.size(); g++) {
                int pos = 0;
                for (RoundRobin.Pairing p : schedules.get(g)) {
                    if (p.round() != round) {
                        continue;
                    }
                    List<Entry> members = drawn.get(g);
                    all.add(Match.group(t, groups.get(g), round, pos++, order++, members.get(p.a()), members.get(p.b())));
                }
            }
        }
        matchRepository.saveAll(all);
    }

    /** 1라운드 슬롯 배치로 결승까지 전체 트리를 만들고 BYE를 처리한다 */
    private void buildKnockout(Tournament t, List<Entry> slots, int startOrder) {
        int size = slots.size();
        int rounds = KnockoutLayout.rounds(size);

        // 결승부터 거꾸로 저장해야 하위 라운드가 다음 경기를 참조할 수 있다
        Map<Integer, List<Match>> byRound = new java.util.HashMap<>();
        int[] orderOfRound = new int[rounds + 1];
        int order = startOrder;
        for (int r = 1; r <= rounds; r++) {
            orderOfRound[r] = order;
            order += size >> r;
        }
        for (int r = rounds; r >= 1; r--) {
            int count = size >> r;
            int roundPlayers = size >> (r - 1);
            List<Match> round = new ArrayList<>(count);
            for (int pos = 0; pos < count; pos++) {
                Match m = Match.knockout(t, r, roundPlayers, pos, orderOfRound[r] + pos);
                if (r < rounds) {
                    m.linkNext(byRound.get(r + 1).get(pos / 2), pos % 2 + 1);
                }
                round.add(m);
            }
            byRound.put(r, matchRepository.saveAll(round));
        }

        List<Match> first = byRound.get(1);
        for (int pos = 0; pos < first.size(); pos++) {
            Match m = first.get(pos);
            m.place(1, slots.get(pos * 2));
            m.place(2, slots.get(pos * 2 + 1));
        }
        for (Match m : first) {
            progression.resolveAutomatic(m);
        }
    }

    private Object clubKey(Entry e) {
        return e.getPlayer().getClub() == null ? null : e.getPlayer().getClub().getId();
    }

    private void deleteStage(Long tournamentId, MatchStage stage) {
        List<Match> matches = matchRepository.findByTournamentIdAndStage(tournamentId, stage);
        // 자기참조 FK(next_match_id)를 먼저 끊는다
        matches.forEach(Match::unlinkNext);
        matchRepository.flush();
        matchRepository.deleteAll(matches);
        matchRepository.flush();
    }

    private Tournament lock(Long id) {
        return tournamentRepository.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("대회", id));
    }
}
