package com.tossai.trading.infrastructure.outbox;

import com.tossai.trading.application.port.out.OrderOutboxPort;
import com.tossai.trading.application.service.execution.ExecutionSubmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 디스패처. PENDING 주문을 주기적으로 제출 재시도한다(구현 규칙 12).
 * 동기 제출이 실패해도 이 디스패처가 안전하게 재처리한다.
 */
@Component
public class OrderOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxDispatcher.class);

    private final OrderOutboxPort outbox;
    private final ExecutionSubmitter submitter;

    public OrderOutboxDispatcher(OrderOutboxPort outbox, ExecutionSubmitter submitter) {
        this.outbox = outbox;
        this.submitter = submitter;
    }

    @Scheduled(fixedDelayString = "${trading.outbox.dispatch-interval-ms:5000}")
    public void dispatch() {
        List<String> pending = outbox.fetchPendingOrderIds(20);
        for (String orderId : pending) {
            try {
                submitter.submit(orderId);
            } catch (Exception e) {
                log.warn("Outbox 재시도 실패 order={} : {}", orderId, e.getMessage());
                outbox.markFailed(orderId, e.getMessage());
            }
        }
    }
}
