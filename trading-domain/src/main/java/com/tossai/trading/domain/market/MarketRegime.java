package com.tossai.trading.domain.market;

/** 시장 국면. AI 트레이더의 국면 판단 결과. */
public enum MarketRegime {
    BULL,
    BEAR,
    SIDEWAYS,
    HIGH_VOLATILITY,
    EVENT
}
