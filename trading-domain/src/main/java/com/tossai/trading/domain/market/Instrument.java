package com.tossai.trading.domain.market;

import java.math.BigDecimal;

/**
 * 종목 마스터 + 현재 시장 상태. market-data 가 공급한다.
 * 거래정지/관리종목/유동성 부족/거래시간 여부를 Risk Engine 이 검증에 사용한다.
 */
public record Instrument(
        String symbol,
        String sector,
        BigDecimal lastPrice,
        boolean tradable,      // 거래 가능 시간 + 정상 상태
        boolean halted,        // 거래정지/관리종목
        boolean illiquid       // 유동성 부족(호가잔량 미달 등)
) {
    public boolean blocksNewEntry() {
        return halted || illiquid || !tradable;
    }
}
