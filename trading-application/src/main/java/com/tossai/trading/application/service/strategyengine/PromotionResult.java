package com.tossai.trading.application.service.strategyengine;

import java.util.List;

/** 백테스트 제출 후 자동주문 승격 판정 결과. */
public record PromotionResult(
        String strategyId,
        boolean autoEligible,
        List<String> failedCriteria
) {
}
