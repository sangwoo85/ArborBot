package com.tossai.trading.domain.signal;

import com.tossai.trading.domain.market.MarketRegime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * AI 트레이더의 판단 결과. 실행 가능한 주문이 아니라 "신호"이다.
 * AI는 이 객체만 생성하며 주문 API를 호출하지 않는다.
 *
 * <p>불변 값 객체. modelVersion/strategyVersion 으로 판단 근거의 버전을 추적한다.
 */
public record TradingSignal(
        String signalId,
        String strategyId,
        String strategyVersion,
        String modelVersion,
        String symbol,
        SignalType signalType,
        int confidenceScore,                 // 0 ~ 100
        BigDecimal recommendedPositionSizePercent,
        BigDecimal entryPriceMin,
        BigDecimal entryPriceMax,
        BigDecimal stopLossPrice,
        BigDecimal takeProfitPrice,
        String holdingPeriod,
        Instant validUntil,
        MarketRegime marketRegime,
        String rationale,
        List<String> riskFlags,
        Instant createdAt
) {

    public boolean isExpired(Instant now) {
        return validUntil != null && now.isAfter(validUntil);
    }

    public boolean isActionable() {
        return signalType == SignalType.BUY || signalType == SignalType.SELL;
    }

    /** 신뢰도/정합성 기본 검증. Signal Quality Gate 의 1차 검사. */
    public boolean isStructurallyValid() {
        if (confidenceScore < 0 || confidenceScore > 100) {
            return false;
        }
        if (signalType == SignalType.BUY && stopLossPrice != null && entryPriceMin != null
                && stopLossPrice.compareTo(entryPriceMin) >= 0) {
            return false; // 매수인데 손절가가 진입가 이상이면 비정합
        }
        return true;
    }
}
