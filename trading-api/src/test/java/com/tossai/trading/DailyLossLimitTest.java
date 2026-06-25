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
 * 매도 체결로 발생한 실현 손실이 일일 손실 한도에 누적되고,
 * 한도 도달 후 신규 매수가 차단됨을 검증한다(자본 손실 통제 실연동).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_loss;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0",
        "trading.limits.max-daily-loss=40000"
})
class DailyLossLimitTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;

    @Test
    void sellLoss_thenBuyBlockedByDailyLossLimit() {
        riskEngine.disableGlobalKillSwitch("setup", "test");

        // 1) 005930 매도(평균가 80000 > 현재가 70000) → 실현 손실 50,000 발생
        TradingSignal sell = aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.SELL, null, "SWING"));
        Order sellOrder = orderService.createOrderFromSignal(sell.signalId());
        Order filled = orderService.approveOrder(sellOrder.getOrderId());
        assertEquals(OrderStatus.FILLED, filled.getStatus());

        // 2) 실현 손실(50,000) >= 한도(40,000) → 신규 매수 거절
        TradingSignal buy = aiTrader.generateSignal(new GenerateSignalCommand(
                "000660", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order buyOrder = orderService.createOrderFromSignal(buy.signalId());
        assertEquals(OrderStatus.REJECTED, buyOrder.getStatus());
        assertTrue(buyOrder.getRejectReason().contains("MAX_DAILY_LOSS"));
    }
}
