package com.parkminje.pingpong.live;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 트랜잭션이 커밋된 뒤에만 WebSocket 으로 전달한다.
 * (커밋 전에 보내면 클라이언트가 바뀌기 전 데이터를 다시 읽는 문제가 생긴다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveEventRelay {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(LiveEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", event.type().name());
        payload.put("tournamentId", event.tournamentId());
        if (event.matchId() != null) {
            payload.put("matchId", event.matchId());
        }
        if (event.message() != null) {
            payload.put("message", event.message());
        }
        try {
            messagingTemplate.convertAndSend("/topic/tournament/" + event.tournamentId(), payload);
        } catch (RuntimeException e) {
            // 실시간 알림 실패가 업무 처리를 되돌리면 안 된다
            log.warn("실시간 이벤트 전송 실패: {}", event, e);
        }
    }
}
