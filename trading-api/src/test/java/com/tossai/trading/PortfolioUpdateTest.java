package com.tossai.trading;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.application.service.execution.OrderService;
import com.tossai.trading.application.service.portfolio.PortfolioService;
import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.portfolio.Portfolio;
import com.tossai.trading.domain.portfolio.Position;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 체결이 포지션/현금에 반영되는지 검증(쓰기 측 폐루프). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_pf;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0",
        "trading.limits.max-order-amount=2000000"
})
class PortfolioUpdateTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired PortfolioService portfolioService;
    @Autowired RiskEngine riskEngine;

    @Test
    void buyFill_increasesPosition_reducesCash() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        Portfolio before = portfolioService.getPortfolio();
        long qty0 = qtyOf(before, "005930");

        TradingSignal buy = aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order order = orderService.approveOrder(
                orderService.createOrderFromSignal(buy.signalId()).getOrderId());
        assertEquals(OrderStatus.FILLED, order.getStatus());

        Portfolio after = portfolioService.getPortfolio();
        assertTrue(after.cash().compareTo(before.cash()) < 0, "현금이 줄어야 함");
        assertTrue(qtyOf(after, "005930") > qty0, "포지션 수량이 늘어야 함");
    }

    @Test
    void sellFill_decreasesPosition_increasesCash() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        Portfolio before = portfolioService.getPortfolio();
        Position pos0 = before.findPosition("000660");
        assertNotNull(pos0);

        TradingSignal sell = aiTrader.generateSignal(new GenerateSignalCommand(
                "000660", "momentum-v1", "v1", SignalType.SELL, null, "SWING"));
        Order order = orderService.approveOrder(
                orderService.createOrderFromSignal(sell.signalId()).getOrderId());
        assertEquals(OrderStatus.FILLED, order.getStatus());

        Portfolio after = portfolioService.getPortfolio();
        assertTrue(after.cash().compareTo(before.cash()) > 0, "현금이 늘어야 함");
        long qtyAfter = qtyOf(after, "000660");
        assertTrue(qtyAfter < pos0.quantity(), "포지션 수량이 줄어야 함");
    }

    private long qtyOf(Portfolio p, String symbol) {
        Position pos = p.findPosition(symbol);
        return pos == null ? 0 : pos.quantity();
    }
}
