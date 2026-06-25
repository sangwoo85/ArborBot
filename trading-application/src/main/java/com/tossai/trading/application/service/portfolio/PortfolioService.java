package com.tossai.trading.application.service.portfolio;

import com.tossai.trading.application.port.out.MarketDataPort;
import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.order.OrderSide;
import com.tossai.trading.domain.portfolio.Portfolio;
import com.tossai.trading.domain.portfolio.PortfolioDecision;
import com.tossai.trading.domain.portfolio.Position;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 포트폴리오 서비스. 현재 포트폴리오 조회 및 신호의 포트폴리오 영향 분석.
 * 체결 가정 시 종목/섹터/현금 비중을 사후(post-trade) 기준으로 계산한다.
 */
@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final MarketDataPort marketDataPort;

    public PortfolioService(PortfolioRepository portfolioRepository, MarketDataPort marketDataPort) {
        this.portfolioRepository = portfolioRepository;
        this.marketDataPort = marketDataPort;
    }

    public Portfolio getPortfolio() {
        return portfolioRepository.getCurrent();
    }

    /**
     * 신호 → 포트폴리오 결정. 목표 수량/가격 및 사후 예상 비중을 계산한다.
     * recommendedPositionSizePercent 는 권장값이며 한도는 Risk Engine 이 최종 적용한다.
     */
    public PortfolioDecision analyzeImpact(TradingSignal signal) {
        Portfolio portfolio = portfolioRepository.getCurrent();
        OrderSide side = signal.signalType() == SignalType.SELL ? OrderSide.SELL : OrderSide.BUY;

        BigDecimal refPrice = referencePrice(signal);
        BigDecimal total = portfolio.totalEvaluation();

        long targetQty;
        if (side == OrderSide.SELL) {
            Position pos = portfolio.findPosition(signal.symbol());
            targetQty = pos == null ? 0 : pos.sellableQuantity();
        } else {
            BigDecimal alloc = total.multiply(nz(signal.recommendedPositionSizePercent()))
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
            targetQty = (refPrice.signum() > 0)
                    ? alloc.divide(refPrice, 0, RoundingMode.DOWN).longValue()
                    : 0;
        }

        BigDecimal orderAmount = refPrice.multiply(BigDecimal.valueOf(targetQty));
        BigDecimal signedDelta = side == OrderSide.BUY ? orderAmount : orderAmount.negate();
        BigDecimal expectedPositionPct = pct(currentSymbolValue(portfolio, signal.symbol())
                .add(signedDelta), total);

        // 섹터 비중: 같은 섹터 보유 평가금액 합계 + 이번 주문 영향 / 총액 (정확 계산)
        String sector = marketDataPort.getInstrument(signal.symbol())
                .map(i -> i.sector()).orElse("UNKNOWN");
        BigDecimal sectorValue = currentSectorValue(portfolio, sector).add(signedDelta);
        BigDecimal expectedSectorPct = pct(sectorValue, total);

        BigDecimal expectedCashReservePct = pct(
                portfolio.cash().subtract(side == OrderSide.BUY ? orderAmount : BigDecimal.ZERO), total);

        return new PortfolioDecision(
                Ids.newId("pf-decision"),
                signal.signalId(),
                signal.signalId(),
                signal.symbol(),
                side,
                targetQty,
                refPrice,
                expectedPositionPct,
                expectedSectorPct,
                expectedCashReservePct,
                Instant.now()
        );
    }

    private BigDecimal referencePrice(TradingSignal signal) {
        if (signal.entryPriceMax() != null && signal.entryPriceMin() != null) {
            return signal.entryPriceMin().add(signal.entryPriceMax())
                    .divide(new BigDecimal("2"), 0, RoundingMode.HALF_UP);
        }
        if (signal.entryPriceMax() != null) {
            return signal.entryPriceMax();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal currentSymbolValue(Portfolio p, String symbol) {
        Position pos = p.findPosition(symbol);
        return pos == null || pos.evaluationAmount() == null ? BigDecimal.ZERO : pos.evaluationAmount();
    }

    private BigDecimal currentSectorValue(Portfolio p, String sector) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Position pos : p.positions()) {
            if (sector.equals(pos.sector()) && pos.evaluationAmount() != null) {
                sum = sum.add(pos.evaluationAmount());
            }
        }
        return sum;
    }

    private BigDecimal pct(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
