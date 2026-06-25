package com.tossai.trading;

import com.tossai.trading.application.service.aitrader.AiTraderService;
import com.tossai.trading.application.service.aitrader.GenerateSignalCommand;
import com.tossai.trading.application.service.execution.ExecutionSubmitter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 부분 체결 처리: 청크 단위로 체결되며 잔량이 추적되고, 폴링을 통해 전량 체결로 수렴한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trading_partial;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "trading.limits.min-confidence-score=0"
})
class PartialFillTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;
    @Autowired ExecutionSubmitter submitter;
    @Autowired MockTossBrokerAdapter broker;

    @Test
    void partialFills_thenConvergesToFilled() {
        riskEngine.disableGlobalKillSwitch("setup", "test");
        broker.setPartialFillChunk(3);   // 3주씩 점진 체결
        try {
            TradingSignal buy = aiTrader.generateSignal(new GenerateSignalCommand(
                    "005930", "momentum-v1", "v1", SignalType.BUY, null, "SWING"));
            Order created = orderService.createOrderFromSignal(buy.signalId());
            long total = created.getQuantity();
            assertTrue(total > 3, "부분 체결 검증을 위해 수량이 청크보다 커야 함");

            // 승인 → 첫 청크만 체결되어 부분 체결 상태
            Order afterApprove = orderService.approveOrder(created.getOrderId());
            assertEquals(OrderStatus.PARTIALLY_FILLED, afterApprove.getStatus());
            assertTrue(afterApprove.getFilledQuantity() > 0
                    && afterApprove.getFilledQuantity() < total, "잔량이 남아야 함");

            // 폴링 반복 → 잔량 누적 체결 → 전량 체결로 수렴
            Order current = afterApprove;
            for (int i = 0; i < 6 && current.getStatus() != OrderStatus.FILLED; i++) {
                submitter.submit(current.getOrderId());
                current = orderService.getOrder(current.getOrderId());
            }
            assertEquals(OrderStatus.FILLED, current.getStatus());
            assertEquals(total, current.getFilledQuantity());
        } finally {
            broker.setPartialFillChunk(0);
        }
    }
}
