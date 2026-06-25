package com.tossai.trading.infrastructure.batch;

import com.tossai.trading.application.service.batch.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * batch-service 스케줄러. 운영 환경에서는 cron 으로 장 시작/종료/일일 시점에 맞춘다.
 * (간격은 application.yml 의 trading.batch.* 로 조정)
 */
@Component
public class ScheduledBatchJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBatchJobs.class);

    private final BatchService batchService;

    public ScheduledBatchJobs(BatchService batchService) {
        this.batchService = batchService;
    }

    /** 주문 상태 동기화(결과 불명 주문 재동기화). */
    @Scheduled(fixedDelayString = "${trading.batch.order-sync-interval-ms:15000}")
    public void syncOrders() {
        int n = batchService.syncSubmittedOrders();
        if (n > 0) {
            log.info("[BATCH] 주문 상태 동기화 대상 {}건", n);
        }
    }

    /** 전략 성과 재평가(데모: 주기 실행, 운영: 일일 cron 권장). */
    @Scheduled(fixedDelayString = "${trading.batch.strategy-eval-interval-ms:3600000}")
    public void reevaluateStrategies() {
        List<String> deactivated = batchService.reevaluateStrategies();
        if (!deactivated.isEmpty()) {
            log.warn("[BATCH] 비활성화된 전략: {}", deactivated);
        }
    }

    /** T+2 결제 처리(운영: 장 시작/일일 시점 cron 권장). */
    @Scheduled(fixedDelayString = "${trading.batch.settlement-interval-ms:3600000}")
    public void settle() {
        int n = batchService.settleDueLots();
        if (n > 0) {
            log.info("[BATCH] 결제 완료(매도가능 전환) 로트 {}건", n);
        }
    }
}
