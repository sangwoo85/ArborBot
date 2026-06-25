package com.tossai.trading.application.port.out;

import java.util.List;

/**
 * 주문 제출 Outbox. 승인된 주문의 제출 의도를 먼저 기록(enqueue)한 뒤 제출한다.
 * 제출 실패 시 PENDING 으로 남아 Dispatcher 가 안전하게 재시도한다(구현 규칙 12).
 */
public interface OrderOutboxPort {
    void enqueue(String orderId, String idempotencyKey);

    void markProcessed(String orderId);

    void markFailed(String orderId, String reason);

    List<String> fetchPendingOrderIds(int limit);
}
