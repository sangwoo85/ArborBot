package com.tossai.trading.api.web;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.domain.signal.TradingSignal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AI 트레이더 판단 이력 조회. */
@RestController
@RequestMapping("/api/v1/ai-trader")
public class AiTraderController {

    private final AiTraderService aiTraderService;

    public AiTraderController(AiTraderService aiTraderService) {
        this.aiTraderService = aiTraderService;
    }

    @GetMapping("/decisions")
    public List<TradingSignal> decisions(@RequestParam(defaultValue = "50") int limit) {
        return aiTraderService.recentDecisions(limit);
    }
}
