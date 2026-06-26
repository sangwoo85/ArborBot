package com.tossai.trading;

import com.tossai.trading.application.port.out.SettlementRepository;
import com.tossai.trading.application.service.portfolio.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 보유하지 않는 종목(전량 매도 후)의 미결제 로트가 정리됨을 검증(FIFO purge).
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:trading_purge;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class SettlementPurgeTest {

    @Autowired SettlementService settlementService;
    @Autowired SettlementRepository settlementRepository;

    @Test
    void unsettledLots_purgedWhenNoPositionHeld() {
        // 보유하지 않는 가상 종목의 결제 로트 기록(10일 전 매수 → 이미 결제일 도래)
        settlementService.recordBuy("PURGE_SYM", 5, LocalDate.now().minusDays(10));
        assertEquals(1, settlementRepository.findUnsettledBySymbol("PURGE_SYM").size());

        // 해당 종목 보유가 없으므로 onSell 시 미결제 로트 정리
        settlementService.onSell("PURGE_SYM");
        assertEquals(0, settlementRepository.findUnsettledBySymbol("PURGE_SYM").size());

        // 정리되었으므로 결제 처리 대상도 없음
        assertEquals(0, settleDueForSymbolCount());
    }

    private int settleDueForSymbolCount() {
        // PURGE_SYM 로트가 없으니 결제 처리 0건(다른 시드 종목 영향 배제 위해 미결제 조회로 확인)
        return settlementRepository.findUnsettledBySymbol("PURGE_SYM").size();
    }
}
