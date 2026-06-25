package com.tossai.trading.domain.portfolio;

import java.math.BigDecimal;
import java.util.List;

/** 포트폴리오 스냅샷: 현금, 주문가능금액, 보유 포지션. */
public record Portfolio(
        String accountId,
        BigDecimal cash,
        BigDecimal orderableAmount,
        List<Position> positions
) {

    public BigDecimal totalEvaluation() {
        BigDecimal sum = cash == null ? BigDecimal.ZERO : cash;
        for (Position p : positions) {
            if (p.evaluationAmount() != null) {
                sum = sum.add(p.evaluationAmount());
            }
        }
        return sum;
    }

    public Position findPosition(String symbol) {
        return positions.stream().filter(p -> p.symbol().equals(symbol)).findFirst().orElse(null);
    }
}
