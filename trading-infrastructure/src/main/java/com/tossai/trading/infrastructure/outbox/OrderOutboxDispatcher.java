package com.tossai.trading.infrastructure.outbox;

import com.tossai.trading.application.port.out.DistributedLockPort;
import com.tossai.trading.application.port.out.OrderOutboxPort;
import com.tossai.trading.application.service.execution.ExecutionSubmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Outbox 디스패처. PENDING 주문을 주기적으로 제출 재시도한다(구현 규칙 12).
 * 동기 제출이 실패해도 이 디스패처가 안전하게 재처리한다.
 */
@Component
public class OrderOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxDispatcher.class);

    private static final String LOCK_KEY = "outbox-dispatch";

    private final OrderOutboxPort outbox;
    private final ExecutionSubmitter submitter;
    private final DistributedLockPort lock;

    public OrderOutboxDispatcher(OrderOutboxPort outbox, ExecutionSubmitter submitter,
                                 DistributedLockPort lock) {
        this.outbox = outbox;
        this.submitter = submitter;
        this.lock = lock;
    }

    @Scheduled(fixedDelayString = "${trading.outbox.dispatch-interval-ms:5000}")
    public void dispatch() {
        // 분산 락: 다중 인스턴스에서도 한 번에 하나만 디스패치(중복 제출 방지).
        Optional<String> token = lock.tryLock(LOCK_KEY, 30);
        if (token.isEmpty()) {
            return;
        }
        try {
            List<String> pending = outbox.fetchPendingOrderIds(20);
            for (String orderId : pending) {
                try {
                    submitter.submit(orderId);
                } catch (Exception e) {
                    log.warn("Outbox 재시도 실패 order={} : {}", orderId, e.getMessage());
                    outbox.markFailed(orderId, e.getMessage());
                }
            }
        } finally {
            lock.unlock(LOCK_KEY, token.get());
        }
    }
}
