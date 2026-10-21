package com.parkminje.pingpong.match;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.live.LiveEvent;
import com.parkminje.pingpong.notification.Notification;
import com.parkminje.pingpong.notification.NotificationService;
import com.parkminje.pingpong.player.RatingService;
import com.parkminje.pingpong.tournament.Tournament;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 본선 트리 진행 규칙. 호출하는 쪽에서 대회 락을 잡고 있어야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchProgression {

    private final RatingService ratingService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;

    /**
     * 경기 자리가 바뀐 뒤 사람이 치를 필요가 없는 경기를 자동 종료한다.
     * <ul>
     *   <li>본선 1라운드에서 한쪽이 비어 있으면 BYE</li>
     *   <li>한쪽이 기권(WITHDRAWN) 상태면 상대 부전승</li>
     * </ul>
     */
    public void resolveAutomatic(Match m) {
        if (m.isCompleted() || m.isInProgress()) {
            return;
        }
        Entry a = m.getEntry1();
        Entry b = m.getEntry2();
        if (m.isKnockout() && m.getRoundNo() == 1 && (a == null) != (b == null)) {
            m.finish(a != null ? a : b, ResultType.BYE, null);
            advance(m);
            return;
        }
        if (a != null && b != null && (a.isWithdrawn() || b.isWithdrawn())) {
            Entry winner = a.isWithdrawn() && !b.isWithdrawn() ? b : a;
            m.finish(winner, ResultType.WALKOVER, null);
            advance(m);
        }
    }

    /** 종료된 경기의 승자를 다음 경기로 올리고, 결승이면 대회를 끝낸다 */
    public void advance(Match m) {
        if (!m.isKnockout() || m.getWinner() == null) {
            return;
        }
        Match next = m.getNextMatch();
        if (next == null) {
            completeTournament(m);
            return;
        }
        next.place(m.getNextSlot(), m.getWinner());
        resolveAutomatic(next);
    }

    /** 결과를 고치거나 취소할 때, 이미 다음 경기로 올라간 승자를 되돌린다 */
    public void retract(Match m) {
        if (!m.isKnockout() || m.getWinner() == null) {
            return;
        }
        Match next = m.getNextMatch();
        Tournament t = m.getTournament();
        if (next == null) {
            if (t.getChampion() != null) {
                ratingService.revert(t);
                t.reopen();
                log.info("결승 결과 수정으로 대회 재개: tournament={}", t.getId());
            }
            return;
        }
        if (!next.isScheduled()) {
            throw new BusinessException("다음 경기(" + next.getRoundLabel() + ")가 이미 진행되었거나 끝났습니다. 다음 경기 결과를 먼저 취소하세요.");
        }
        next.place(m.getNextSlot(), null);
    }

    private void completeTournament(Match fin) {
        Tournament t = fin.getTournament();
        Entry champion = fin.getWinner();
        t.complete(champion);
        ratingService.apply(t);
        notificationService.notifyEntry(champion, Notification.Type.SUCCESS, "우승을 축하합니다!",
                t.getTitle() + " 우승", "/tournament/" + t.getId() + "/bracket");
        events.publishEvent(LiveEvent.of(t.getId(), LiveEvent.Type.TOURNAMENT_UPDATED));
        log.info("대회 종료: tournament={} champion={}", t.getId(), champion.getId());
    }
}
