package com.tossai.trading.api.web;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.common.error.DomainException;
import com.tossai.trading.common.error.ErrorCode;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * AI 신호 API. POST 는 AI 트레이더가 신호를 생성한다.
 * (이 컨트롤러는 주문을 생성하지 않는다 — 신호와 주문의 분리.)
 */
@RestController
@RequestMapping("/api/v1/signals")
public class SignalController {

    private final AiTraderService aiTraderService;

    public SignalController(AiTraderService aiTraderService) {
        this.aiTraderService = aiTraderService;
    }

    @PostMapping
    public TradingSignal create(@RequestBody GenerateSignalRequest req) {
        SignalType requested = req.requestedType() == null ? null : SignalType.valueOf(req.requestedType());
        return aiTraderService.generateSignal(new GenerateSignalCommand(
                req.symbol(), req.strategyId(), req.strategyVersion(), requested,
                req.referencePrice(), req.holdingPeriod()));
    }

    @GetMapping("/{signalId}")
    public TradingSignal get(@PathVariable String signalId) {
        return aiTraderService.findById(signalId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "신호 없음: " + signalId));
    }

    public record GenerateSignalRequest(
            @NotBlank String symbol,
            String strategyId,
            String strategyVersion,
            String requestedType,
            BigDecimal referencePrice,
            String holdingPeriod
    ) {
    }
}
