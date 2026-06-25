package com.tossai.trading.common.error;

/** 도메인/응용 공통 오류 코드. */
public enum ErrorCode {
    NOT_FOUND,
    VALIDATION_FAILED,
    ILLEGAL_STATE_TRANSITION,
    RISK_REJECTED,
    KILL_SWITCH_ACTIVE,
    BROKER_ERROR,
    DUPLICATE
}
