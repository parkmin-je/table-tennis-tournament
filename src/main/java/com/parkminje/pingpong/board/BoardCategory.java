package com.parkminje.pingpong.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardCategory {
    NOTICE("공지사항", true),
    FREE("자유게시판", false),
    RESULT("대회 후기", false);

    private final String label;
    /** 운영진만 글을 쓸 수 있는 게시판 */
    private final boolean staffOnly;
}
