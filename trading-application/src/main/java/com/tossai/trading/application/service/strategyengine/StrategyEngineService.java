package com.tossai.trading.application.service.strategyengine;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.NotificationPort;
import com.tossai.trading.application.port.out.StrategyPerformanceRepository;
import com.tossai.trading.application.port.out.StrategyRepository;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import com.tossai.trading.domain.strategy.StrategyThresholds;
import com.tossai.trading.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 전략 엔진. 전략 목록/성과 제공, 활성 전략 선택, 자동주문 적격성 판정.
 * 성과가 기준 이하인 전략은 자동 주문 대상에서 제외한다(구현 규칙 14).
 */
@Service
public class StrategyEngineService {

    private final StrategyRepository strategyRepository;
    private final StrategyPerformanceRepository performanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationPort notifications;
    private final StrategyThresholds thresholds = StrategyThresholds.defaults();

    public StrategyEngineService(StrategyRepository strategyRepository,
                                 StrategyPerformanceRepository performanceRepository,
                                 AuditLogRepository auditLogRepository,
                                 NotificationPort notifications) {
        this.strategyRepository = strategyRepository;
        this.performanceRepository = performanceRepository;
        this.auditLogRepository = auditLogRepository;
        this.notifications = notifications;
    }

    public List<TradingStrategy> listStrategies() {
        return strategyRepository.findAll();
    }

    public List<StrategyPerformance> listPerformance() {
        return performanceRepository.findAll();
    }

    /** 활성 + 자동주문 적격 전략만 반환. */
    public List<TradingStrategy> selectAutoEligibleStrategies() {
        return strategyRepository.findAll().stream()
                .filter(TradingStrategy::active)
                .filter(s -> isAutoEligible(s.strategyId()))
                .toList();
    }

    /**
     * 전략 성과 재평가. 성과가 임계 이하인 활성 전략을 비활성화한다(비활성화 기준 충족 시).
     * batch-service 가 주기적으로 호출한다. 비활성화는 감사 로그 + 알림으로 남긴다.
     *
     * @return 이번에 비활성화된 전략 ID 목록
     */
    public List<String> reevaluateAndDeactivate() {
        List<String> deactivated = new ArrayList<>();
        for (TradingStrategy s : strategyRepository.findAll()) {
            if (!s.active()) {
                continue;
            }
            boolean healthy = performanceRepository.findByStrategyId(s.strategyId())
                    .map(p -> p.meetsAutoTradingThreshold(thresholds))
                    .orElse(false);
            if (!healthy) {
                strategyRepository.setActive(s.strategyId(), false);
                auditLogRepository.save(new AuditLog(Ids.newId("audit"), null,
                        "STRATEGY", "DEACTIVATED",
                        "strategyId=" + s.strategyId() + " 성과 기준 미달", "batch", Instant.now()));
                notifications.notify("HIGH", "전략 비활성화",
                        "strategyId=" + s.strategyId() + " 성과 악화로 자동 비활성화", null);
                deactivated.add(s.strategyId());
            }
        }
        return deactivated;
    }

    /** 전략이 자동 주문 대상으로 적격인지(활성 + 성과 임계 충족). */
    public boolean isAutoEligible(String strategyId) {
        Optional<TradingStrategy> strategy = strategyRepository.findById(strategyId);
        if (strategy.isEmpty() || !strategy.get().active() || !strategy.get().autoTradingEligible()) {
            return false;
        }
        return performanceRepository.findByStrategyId(strategyId)
                .map(p -> p.meetsAutoTradingThreshold(thresholds))
                .orElse(false);
    }
}
