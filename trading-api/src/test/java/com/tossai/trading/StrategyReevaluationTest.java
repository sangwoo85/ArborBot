package com.tossai.trading;

import com.tossai.trading.application.service.batch.BatchService;
import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.domain.strategy.TradingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 전략 성과 재평가 배치가 임계 미달 전략을 비활성화하고,
 * 건전한 전략은 유지함을 검증한다(전략 거버넌스 자동화).
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:trading_reeval;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class StrategyReevaluationTest {

    @Autowired BatchService batchService;
    @Autowired StrategyEngineService strategyEngine;

    @Test
    void reevaluation_deactivatesUnderperformers_keepsHealthy() {
        batchService.reevaluateStrategies();   // 멱등: 스케줄러가 이미 실행했어도 결과 상태 동일

        // momentum-v1: 건전 → 활성 유지 / mean-reversion-v1: MDD·연속손실 초과 → 비활성
        assertTrue(active("momentum-v1"), "건전한 전략은 활성 유지");
        assertFalse(active("mean-reversion-v1"), "성과 악화 전략은 비활성화");
        assertFalse(active("experimental-v0"), "표본 부족 전략은 비활성화");
        // 자동주문 적격은 momentum-v1 만
        assertTrue(strategyEngine.isAutoEligible("momentum-v1"));
        assertFalse(strategyEngine.isAutoEligible("mean-reversion-v1"));
    }

    private boolean active(String strategyId) {
        return strategyEngine.listStrategies().stream()
                .filter(s -> s.strategyId().equals(strategyId))
                .map(TradingStrategy::active)
                .findFirst().orElse(false);
    }
}
