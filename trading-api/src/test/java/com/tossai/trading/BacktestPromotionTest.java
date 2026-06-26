package com.tossai.trading;

import com.tossai.trading.application.service.strategyengine.PromotionResult;
import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 백테스트 결과 주입 → 임계 충족 전략만 자동주문 적격으로 승격됨을 검증(폐루프).
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:trading_bt;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class BacktestPromotionTest {

    @Autowired StrategyEngineService strategyEngine;

    private StrategyPerformance perf(String id, double mdd, double sharpe, int trades, int consec) {
        return new StrategyPerformance(id, new BigDecimal("12.0"), BigDecimal.valueOf(mdd),
                new BigDecimal("55.0"), new BigDecimal("1.4"), BigDecimal.valueOf(sharpe),
                trades, new BigDecimal("10.0"), consec);
    }

    @Test
    void goodBacktest_promotesToAutoEligible() {
        strategyEngine.registerStrategy("bt-good-v1", "v1", "백테스트 통과 전략", MarketRegime.BULL);
        assertFalse(strategyEngine.isAutoEligible("bt-good-v1"), "등록 직후는 미적격");

        PromotionResult r = strategyEngine.submitBacktest("bt-good-v1",
                perf("bt-good-v1", 8.0, 1.2, 120, 1));   // MDD 8%, sharpe 1.2, 120거래
        assertTrue(r.autoEligible());
        assertTrue(r.failedCriteria().isEmpty());
        assertTrue(strategyEngine.isAutoEligible("bt-good-v1"), "승격 후 적격");
    }

    @Test
    void poorBacktest_notPromoted() {
        strategyEngine.registerStrategy("bt-bad-v1", "v1", "백테스트 미달 전략", MarketRegime.SIDEWAYS);

        PromotionResult r = strategyEngine.submitBacktest("bt-bad-v1",
                perf("bt-bad-v1", 30.0, 0.2, 8, 0));   // MDD 30%>20, sharpe 0.2<0.5, 8거래<20
        assertFalse(r.autoEligible());
        assertTrue(r.failedCriteria().contains("MAX_DRAWDOWN"));
        assertTrue(r.failedCriteria().contains("MIN_SHARPE"));
        assertTrue(r.failedCriteria().contains("MIN_TRADE_COUNT"));
        assertFalse(strategyEngine.isAutoEligible("bt-bad-v1"));
    }
}
