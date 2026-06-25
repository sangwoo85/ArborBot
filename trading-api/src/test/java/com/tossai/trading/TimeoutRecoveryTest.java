package com.tossai.trading;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.application.service.execution.OrderService;
import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.domain.order.Order;
import com.tossai.trading.domain.order.OrderStatus;
import com.tossai.trading.domain.signal.SignalType;
import com.tossai.trading.domain.signal.TradingSignal;
import com.tossai.trading.infrastructure.broker.MockTossBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 제출 응답이 유실되는 타임아웃 상황에서, 맹목 재전송 없이 주문 조회로 재동기화되어
 * 정상 체결로 복구됨을 검증한다(중복 주문 방지 + 결과 불명 복구).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_timeout;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0"
})
class TimeoutRecoveryTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;
    @Autowired MockTossBrokerAdapter broker;

    @Test
    void placeTimeout_recoveredByQuery_toFilled() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        TradingSignal signal = aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
        Order order = orderService.createOrderFromSignal(signal.signalId());
        assertEquals(OrderStatus.PENDING_APPROVAL, order.getStatus());

        broker.setSimulatePlaceTimeout(true);
        try {
            Order result = orderService.approveOrder(order.getOrderId());
            // place 는 예외였지만 queryOrder 로 접수 확인 → FILLED 로 복구
            assertEquals(OrderStatus.FILLED, result.getStatus());
        } finally {
            broker.setSimulatePlaceTimeout(false);
        }
    }
}
