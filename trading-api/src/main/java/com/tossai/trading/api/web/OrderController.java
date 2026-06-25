package com.tossai.trading.api.web;

import com.tossai.trading.application.service.execution.OrderService;
import com.tossai.trading.domain.order.Order;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 API. 주문은 신호로부터 생성되며, 생성 과정에서 Risk Engine 검증을 거친다.
 * BUY 신호여도 Risk Engine 이 거절하면 REJECTED 로 반환된다(주문 미제출).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order create(@RequestBody CreateOrderRequest req) {
        return orderService.createOrderFromSignal(req.signalId());
    }

    @GetMapping("/{orderId}")
    public Order get(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public Order cancel(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }

    /** 반자동(SEMI_AUTO) 승인 대기 주문을 사람이 승인. */
    @PostMapping("/{orderId}/approve")
    public Order approve(@PathVariable String orderId) {
        return orderService.approveOrder(orderId);
    }

    public record CreateOrderRequest(@NotBlank String signalId) {
    }
}
