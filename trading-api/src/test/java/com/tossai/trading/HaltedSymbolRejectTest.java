package com.tossai.trading;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.application.service.execution.OrderService;
import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 거래정지/유동성 부족 종목에 대한 신규 매수는 market-data 상태로 거절된다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_tradable;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0"
})
class HaltedSymbolRejectTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;

    @Test
    void haltedSymbol_buyRejected() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        TradingSignal signal = aiTrader.generateSignal(new GenerateSignalCommand(
                "900110", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order order = orderService.createOrderFromSignal(signal.signalId());
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertTrue(order.getRejectReason().contains("HALTED_SYMBOL"));
    }

    @Test
    void illiquidSymbol_buyRejected() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        TradingSignal signal = aiTrader.generateSignal(new GenerateSignalCommand(
                "900120", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order order = orderService.createOrderFromSignal(signal.signalId());
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertTrue(order.getRejectReason().contains("ILLIQUID_OR_VOLATILE"));
    }
}
