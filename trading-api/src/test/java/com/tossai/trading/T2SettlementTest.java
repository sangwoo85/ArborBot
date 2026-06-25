package com.tossai.trading;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.application.service.execution.OrderService;
import com.tossai.trading.application.service.portfolio.PortfolioService;
import com.tossai.trading.application.service.portfolio.SettlementService;
import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.portfolio.Position;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T+2 결제: 매수 직후에는 매도가능수량이 늘지 않고, 결제일 이후 배치로 전환됨을 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_settle;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0"
})
class T2SettlementTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;
    @Autowired PortfolioService portfolioService;
    @Autowired SettlementService settlementService;

    @Test
    void buyNotSellableUntilSettled() {
        riskEngine.disableGlobalKillSwitch("setup", "test");

        long sellableBefore = sellable("005930");   // 시드: 5
        long qtyBefore = quantity("005930");

        TradingSignal buy = aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order order = orderService.approveOrder(
                orderService.createOrderFromSignal(buy.signalId()).getOrderId());
        assertEquals(OrderStatus.FILLED, order.getStatus());
        long bought = order.getFilledQuantity();

        // 매수 직후: 보유수량은 늘지만 매도가능수량은 그대로(T+2 전)
        assertEquals(qtyBefore + bought, quantity("005930"));
        assertEquals(sellableBefore, sellable("005930"), "결제 전에는 매도가능수량 불변");

        // 결제일 도래분 처리 → 매도가능수량 증가
        int settled = settlementService.settleDue(LocalDate.now().plusDays(5));
        assertEquals(1, settled);
        assertEquals(sellableBefore + bought, sellable("005930"), "결제 후 매도가능수량 증가");
    }

    private long sellable(String symbol) {
        Position p = portfolioService.getPortfolio().findPosition(symbol);
        return p == null ? 0 : p.sellableQuantity();
    }

    private long quantity(String symbol) {
        Position p = portfolioService.getPortfolio().findPosition(symbol);
        return p == null ? 0 : p.quantity();
    }
}
