package com.parkminje.pingpong.common;

/** 사용자에게 그대로 보여줘도 되는 업무 규칙 위반 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
