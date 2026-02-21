package com.yourcompany.pingpong.domain;

public enum BoardCategory {
    NOTICE("공지사항"),
    FREE("자유게시판");

    private final String description;

    BoardCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
