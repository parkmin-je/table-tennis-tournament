package com.parkminje.pingpong.common;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + "을(를) 찾을 수 없습니다. (ID: " + id + ")");
    }
}
