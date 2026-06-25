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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 자동 주문 허용 조건이 모두 충족되면 사람 승인 없이 자동 제출/체결된다(구현 규칙 5).
 * 별도 인메모리 DB 로 격리한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_auto;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.auto-trading-enabled=true",
        "trading.approval-required=false",
        "trading.mode=AUTO",
        "trading.limits.auto-confidence-score=50",
        "trading.limits.auto-order-amount-cap=2000000",
        "trading.limits.max-order-amount=2000000"
})
class AutoTradingFlowTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;

    @Test
    void autoEligible_submitsAndFillsWithoutApproval() {
        riskEngine.disableGlobalKillSwitch("test-setup", "test");
        TradingSignal signal = aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.BUY, new BigDecimal("70000"), "SWING"));

        Order order = orderService.createOrderFromSignal(signal.signalId());
        assertEquals(OrderStatus.FILLED, order.getStatus());
    }
}
