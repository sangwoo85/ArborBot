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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 기본 설정(approvalRequired=true, autoTradingEnabled=false)에서의 흐름과
 * Kill Switch 거절을 검증한다.
 */
@SpringBootTest
class SignalOrderFlowTest {

    @Autowired AiTraderService aiTrader;
    @Autowired OrderService orderService;
    @Autowired RiskEngine riskEngine;

    private TradingSignal buySignal() {
        return aiTrader.generateSignal(new GenerateSignalCommand(
                "005930", "momentum-v1", "v1", SignalType.BUY, new BigDecimal("70000"), "SWING"));
    }

    @Test
    void defaultFlow_goesToPendingApproval_thenApproveFills() {
        riskEngine.disableGlobalKillSwitch("test-setup", "test"); // 깨끗한 상태 보장
        TradingSignal signal = buySignal();

        Order order = orderService.createOrderFromSignal(signal.signalId());
        // 기본은 승인 필요 → 자동 제출되지 않고 PENDING_APPROVAL
        assertEquals(OrderStatus.PENDING_APPROVAL, order.getStatus());

        Order approved = orderService.approveOrder(order.getOrderId());
        // Mock broker 가 전량 체결 → FILLED
        assertEquals(OrderStatus.FILLED, approved.getStatus());
        assertTrue(approved.getFilledQuantity() > 0);
    }

    @Test
    void killSwitchOn_rejectsOrder() {
        riskEngine.enableGlobalKillSwitch("test", "test");
        try {
            TradingSignal signal = buySignal();
            Order order = orderService.createOrderFromSignal(signal.signalId());
            assertEquals(OrderStatus.REJECTED, order.getStatus());
            assertTrue(order.getRejectReason().contains("KILL_SWITCH"));
        } finally {
            riskEngine.disableGlobalKillSwitch("test-teardown", "test");
        }
    }
}
