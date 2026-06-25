package com.tossai.trading.common.util;

import java.util.UUID;

/** 추적 식별자 생성 유틸. correlationId, signalId, orderId 등에 사용. */
public final class Ids {

    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** 접두사를 붙인 식별자(가독성). 예: order-xxxx */
    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
