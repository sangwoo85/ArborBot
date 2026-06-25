package com.tossai.trading.application.service.aitrader;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.TradingSignalRepository;
import com.tossai.trading.application.service.marketdata.MarketDataService;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.market.MarketRegime;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * AI 트레이더. 시장 국면을 판단하고 TradingSignal 을 생성한다.
 * <b>이 서비스는 BrokerPort/OrderService 에 의존하지 않으며 주문 API 를 호출하지 않는다(구현 규칙 1).</b>
 *
 * <p>실제 모델 연동 전까지는 결정적(deterministic) Mock 휴리스틱으로 신호를 생성한다.
 * 판단 근거(rationale)와 modelVersion/strategyVersion 을 함께 저장한다(구현 규칙 13).
 */
@Service
public class AiTraderService {

    private static final String MODEL_VERSION = "mock-heuristic-1.0.0";

    private final TradingSignalRepository signalRepository;
    private final AuditLogRepository auditLogRepository;
    private final MarketDataService marketDataService;

    public AiTraderService(TradingSignalRepository signalRepository,
                           AuditLogRepository auditLogRepository,
                           MarketDataService marketDataService) {
        this.signalRepository = signalRepository;
        this.auditLogRepository = auditLogRepository;
        this.marketDataService = marketDataService;
    }

    /** 간단한 국면 판단(Mock). 실제로는 지수/변동성/지표를 종합한다. */
    public MarketRegime detectRegime(String symbol) {
        int h = Math.floorMod(symbol.hashCode(), 5);
        return switch (h) {
            case 0 -> MarketRegime.BULL;
            case 1 -> MarketRegime.BEAR;
            case 2 -> MarketRegime.SIDEWAYS;
            case 3 -> MarketRegime.HIGH_VOLATILITY;
            default -> MarketRegime.EVENT;
        };
    }

    /**
     * 신호 생성. requestedType 이 주어지면 해당 방향으로, 아니면 국면 기반으로 결정한다.
     * 참조 가격(referencePrice)을 기준으로 진입/손절/익절을 산출한다.
     */
    public TradingSignal generateSignal(GenerateSignalCommand cmd) {
        Instant now = Instant.now();
        MarketRegime regime = detectRegime(cmd.symbol());

        SignalType type = cmd.requestedType() != null
                ? cmd.requestedType()
                : switch (regime) {
                    case BULL -> SignalType.BUY;
                    case BEAR -> SignalType.SELL;
                    default -> SignalType.HOLD;
                };

        // 참조가: 시장 현재가 우선, 없으면 요청에 담긴 fallback 사용
        BigDecimal ref = marketDataService.referencePrice(cmd.symbol(), cmd.referencePrice());
        int confidence = computeConfidence(regime, type);

        BigDecimal entryMin = null, entryMax = null, stop = null, take = null;
        BigDecimal recommendedPct = BigDecimal.ZERO;

        if (type == SignalType.BUY && ref != null) {
            entryMin = scale(ref.multiply(new BigDecimal("0.995")));
            entryMax = scale(ref.multiply(new BigDecimal("1.005")));
            stop = scale(ref.multiply(new BigDecimal("0.95")));   // -5% 손절
            take = scale(ref.multiply(new BigDecimal("1.10")));   // +10% 익절
            recommendedPct = new BigDecimal("5");
        } else if (type == SignalType.SELL && ref != null) {
            entryMin = scale(ref.multiply(new BigDecimal("0.995")));
            entryMax = scale(ref.multiply(new BigDecimal("1.005")));
            recommendedPct = BigDecimal.ZERO;
        }

        List<String> riskFlags = regime == MarketRegime.HIGH_VOLATILITY
                ? List.of("ELEVATED_VOLATILITY")
                : (regime == MarketRegime.EVENT ? List.of("EVENT_RISK") : List.of());

        String rationale = "regime=" + regime + ", type=" + type
                + ", confidence=" + confidence + " (Mock heuristic). 참조가=" + ref;

        TradingSignal signal = new TradingSignal(
                Ids.newId("signal"),
                cmd.strategyId(),
                cmd.strategyVersion() == null ? "v1" : cmd.strategyVersion(),
                MODEL_VERSION,
                cmd.symbol(),
                type,
                confidence,
                recommendedPct,
                entryMin, entryMax, stop, take,
                cmd.holdingPeriod() == null ? "SWING" : cmd.holdingPeriod(),
                now.plus(Duration.ofHours(6)),
                regime,
                rationale,
                riskFlags,
                now
        );

        TradingSignal saved = signalRepository.save(signal);
        auditLogRepository.save(new AuditLog(Ids.newId("audit"), saved.signalId(),
                "SIGNAL", "CREATED",
                "type=" + type + ", symbol=" + cmd.symbol() + ", conf=" + confidence,
                "ai-trader", now));
        return saved;
    }

    public List<TradingSignal> recentDecisions(int limit) {
        return signalRepository.findRecent(limit);
    }

    public java.util.Optional<TradingSignal> findById(String signalId) {
        return signalRepository.findById(signalId);
    }

    private int computeConfidence(MarketRegime regime, SignalType type) {
        if (type == SignalType.HOLD) {
            return 50;
        }
        return switch (regime) {
            case BULL, BEAR -> 78;
            case SIDEWAYS -> 65;
            case HIGH_VOLATILITY -> 58;
            case EVENT -> 55;
        };
    }

    private BigDecimal scale(BigDecimal v) {
        return v.setScale(0, java.math.RoundingMode.HALF_UP);
    }
}
