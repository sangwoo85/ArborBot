package com.tossai.trading.api.web;

import com.tossai.trading.application.service.strategyengine.StrategyEngineService;
import com.tossai.trading.domain.strategy.StrategyPerformance;
import com.tossai.trading.domain.strategy.TradingStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
