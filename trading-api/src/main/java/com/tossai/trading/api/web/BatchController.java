package com.tossai.trading.api.web;

import com.tossai.trading.application.service.batch.BatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 배치 수동 트리거(운영/점검용). 스케줄러와 동일 로직을 즉시 실행한다. */
@RestController
@RequestMapping("/api/v1/batch")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/sync-orders")
    public Map<String, Object> syncOrders() {
        return Map.of("synced", batchService.syncSubmittedOrders());
    }

    @PostMapping("/reevaluate-strategies")
    public Map<String, Object> reevaluate() {
        List<String> deactivated = batchService.reevaluateStrategies();
        return Map.of("deactivated", deactivated);
    }

    @PostMapping("/daily-settlement")
    public BatchService.DailySettlement settlement() {
        return batchService.dailySettlement();
    }
}
