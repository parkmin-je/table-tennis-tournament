package com.parkminje.pingpong.live;

/**
 * 대회 화면(현황판·운영 콘솔·상세)에 "바뀌었으니 다시 불러오라"고 알리는 신호.
 * 데이터 자체는 보내지 않고, 클라이언트가 서버 렌더링된 조각을 다시 받아간다.
 */
public record LiveEvent(Long tournamentId, Type type, Long matchId, String message) {

    public enum Type {
        DRAW_CREATED, DRAW_RESET, MATCH_STARTED, MATCH_COMPLETED, MATCH_UPDATED, ENTRY_CHANGED, TOURNAMENT_UPDATED
    }

    public static LiveEvent of(Long tournamentId, Type type) {
        return new LiveEvent(tournamentId, type, null, null);
    }

    public static LiveEvent match(Long tournamentId, Type type, Long matchId, String message) {
        return new LiveEvent(tournamentId, type, matchId, message);
    }
}
