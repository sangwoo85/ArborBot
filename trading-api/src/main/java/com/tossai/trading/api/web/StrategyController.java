package com.tossai.trading.api.web;

import com.tossai.trading.application.service.strategyengine.PromotionResult;
import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import com.tossai.trading.domain.strategy.TradingStrategy;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/strategies")
public class StrategyController {

    private final StrategyEngineService strategyEngine;

    public StrategyController(StrategyEngineService strategyEngine) {
        this.strategyEngine = strategyEngine;
    }

    @GetMapping
    public List<TradingStrategy> list() {
        return strategyEngine.listStrategies();
    }

    @GetMapping("/performance")
    public List<StrategyPerformance> performance() {
        return strategyEngine.listPerformance();
    }

    /** 전략 등록(거버넌스 DRAFT). 자동주문은 백테스트 승격 후 가능. */
    @PostMapping
    public TradingStrategy register(@RequestBody RegisterStrategyRequest req) {
        MarketRegime regime = req.targetRegime() == null
                ? MarketRegime.SIDEWAYS : MarketRegime.valueOf(req.targetRegime());
        return strategyEngine.registerStrategy(req.strategyId(), req.version(), req.description(), regime);
    }

    /**
     * 백테스트 결과 제출 → 임계 충족 시 자동주문 적격으로 승격.
     * research/ 백테스트 리포트(JSON)를 그대로 받는다.
     */
    @PostMapping("/{strategyId}/backtest")
    public PromotionResult submitBacktest(@PathVariable String strategyId,
                                          @RequestBody BacktestReport report) {
        return strategyEngine.submitBacktest(strategyId, report.toPerformance(strategyId));
    }

    public record RegisterStrategyRequest(
            @NotBlank String strategyId,
            String version,
            String description,
            String targetRegime
    ) {
    }

    /** research/ 백테스트 리포트 매핑(필드명은 Python PerformanceReport 와 일치). */
    public record BacktestReport(
            BigDecimal cumulativeReturnPct,
            BigDecimal maxDrawdownPct,
            BigDecimal winRatePct,
            BigDecimal profitFactor,
            BigDecimal sharpeRatio,
            int tradeCount,
            BigDecimal netReturnAfterCostPct,
            Integer consecutiveLosses
    ) {
        StrategyPerformance toPerformance(String strategyId) {
            // profitFactor 가 무한대(손실 0)면 큰 유한값으로 캡(저장 가능하게).
            BigDecimal pf = profitFactor == null ? BigDecimal.ZERO : capFinite(profitFactor);
            return new StrategyPerformance(
                    strategyId, nz(cumulativeReturnPct), nz(maxDrawdownPct), nz(winRatePct), pf,
                    nz(sharpeRatio), tradeCount, nz(netReturnAfterCostPct),
                    consecutiveLosses == null ? 0 : consecutiveLosses);
        }

        private static BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }

        private static BigDecimal capFinite(BigDecimal v) {
            BigDecimal cap = new BigDecimal("9999.9999");
            return v.compareTo(cap) > 0 ? cap : v;
        }
    }
}
