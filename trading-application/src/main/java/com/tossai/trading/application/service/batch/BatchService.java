package com.tossai.trading.application.service.batch;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.DailyRiskUsageRepository;
import com.tossai.trading.application.port.out.NotificationPort;
import com.tossai.trading.application.port.out.OrderRepository;
import com.tossai.trading.application.service.execution.ExecutionSubmitter;
import com.tossai.trading.application.service.portfolio.SettlementService;
import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.order.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * batch-service: 주문 상태 동기화, 전략 성과 재평가, 일일 정산을 담당한다.
 * 스케줄러(infra)와 관리자 API 가 호출한다. 멱등하게 동작한다.
 */
@Service
public class BatchService {

    private final OrderRepository orderRepository;
    private final ExecutionSubmitter submitter;
    private final StrategyEngineService strategyEngine;
    private final SettlementService settlementService;
    private final DailyRiskUsageRepository usageRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationPort notifications;

    public BatchService(OrderRepository orderRepository, ExecutionSubmitter submitter,
                        StrategyEngineService strategyEngine, SettlementService settlementService,
                        DailyRiskUsageRepository usageRepository,
                        AuditLogRepository auditLogRepository, NotificationPort notifications) {
        this.orderRepository = orderRepository;
        this.submitter = submitter;
        this.strategyEngine = strategyEngine;
        this.settlementService = settlementService;
        this.usageRepository = usageRepository;
        this.auditLogRepository = auditLogRepository;
        this.notifications = notifications;
    }

    /**
     * 미완 주문(SUBMITTED: 결과 불명 / PARTIALLY_FILLED: 잔량)을 증권사 조회로 재동기화한다(재전송 없음).
     */
    public int syncSubmittedOrders() {
        List<Order> pending = new java.util.ArrayList<>();
        pending.addAll(orderRepository.findByStatus("SUBMITTED"));
        pending.addAll(orderRepository.findByStatus("PARTIALLY_FILLED"));
        for (Order o : pending) {
            submitter.submit(o.getOrderId());   // 조회 기반 reconcile (추가 체결 반영)
        }
        return pending.size();
    }

    /** 전략 성과 재평가 → 임계 미달 전략 비활성화. */
    public List<String> reevaluateStrategies() {
        return strategyEngine.reevaluateAndDeactivate();
    }

    /** T+2 결제 완료분을 매도가능수량으로 전환. 반환: 처리한 결제 로트 수. */
    public int settleDueLots() {
        return settlementService.settleDue(LocalDate.now());
    }

    /** 일일 정산 요약(주문금액/실현손실)을 감사 로그에 남긴다. */
    public DailySettlement dailySettlement() {
        LocalDate today = LocalDate.now();
        BigDecimal orderAmount = usageRepository.todayOrderAmount(today);
        BigDecimal realizedLoss = usageRepository.todayRealizedLoss(today);
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "SETTLEMENT", "DAILY",
                "date=" + today + " orderAmount=" + orderAmount + " realizedLoss=" + realizedLoss,
                "batch", Instant.now()));
        notifications.notify("INFO", "일일 정산",
                "date=" + today + " 주문금액=" + orderAmount + " 실현손실=" + realizedLoss, null);
        return new DailySettlement(today, orderAmount, realizedLoss);
    }

    public record DailySettlement(LocalDate date, BigDecimal orderAmount, BigDecimal realizedLoss) {
    }
}
