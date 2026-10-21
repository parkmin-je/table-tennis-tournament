package com.parkminje.pingpong.operation;

import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryService;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 현장 접수: 선수 등록과 엔트리 등록을 한 트랜잭션으로 (엔트리 등록이 실패하면 선수도 남기지 않는다) */
@Service
@RequiredArgsConstructor
public class WalkInService {

    private final PlayerService playerService;
    private final EntryService entryService;

    @Transactional
    public Entry register(Long tournamentId, PlayerService.PlayerInput input, User staff, boolean ignoreRestriction) {
        Player p = playerService.create(input);
        return entryService.register(tournamentId, p.getId(), null, staff, ignoreRestriction);
    }
}
