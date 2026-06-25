package com.tossai.trading.api.web;

import com.tossai.trading.application.service.risk.RiskEngine;
import com.tossai.trading.application.service.risk.RiskStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 위험/Kill Switch 운영 API. Kill Switch 조작은 사유와 함께 감사 로그에 기록된다.
 */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskEngine riskEngine;

    public RiskController(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
    }

    @GetMapping("/status")
    public RiskStatus status() {
        return riskEngine.status();
    }

    @PostMapping("/kill-switch/enable")
    public RiskStatus enable(@RequestBody(required = false) KillSwitchRequest req) {
        String reason = req == null || req.reason() == null ? "manual" : req.reason();
        String actor = req == null || req.actor() == null ? "admin" : req.actor();
        riskEngine.enableGlobalKillSwitch(reason, actor);
        return riskEngine.status();
    }

    @PostMapping("/kill-switch/disable")
    public RiskStatus disable(@RequestBody(required = false) KillSwitchRequest req) {
        String reason = req == null || req.reason() == null ? "manual" : req.reason();
        String actor = req == null || req.actor() == null ? "admin" : req.actor();
        riskEngine.disableGlobalKillSwitch(reason, actor);
        return riskEngine.status();
    }

    public record KillSwitchRequest(String reason, String actor) {
    }
}
