package com.agentforge.exception;

import lombok.Getter;

/** 业务异常：消息会透传给前端（api-contract.md §9） */
@Getter
public class BizException extends RuntimeException {

    private static final int DEFAULT_CODE = 400;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = DEFAULT_CODE;
    }
}
