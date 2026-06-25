"""전략: 신호(BUY/SELL/HOLD) 생성. 롱 온리(개인 계좌, 공매도 없음).

ArborBot 의 TradingSignal 과 동일한 개념(방향만 단순화). 각 전략은 현재 시점까지의
히스토리만 보고 결정한다(룩어헤드 금지).
"""
from __future__ import annotations

import statistics
from abc import ABC, abstractmethod

from .data import Bar

BUY = "BUY"
SELL = "SELL"
HOLD = "HOLD"


class Strategy(ABC):
    name: str = "base"

    @abstractmethod
    def decide(self, history: list[Bar], in_position: bool) -> str:
        """history[-1] 이 현재 봉. in_position=현재 보유 여부. BUY/SELL/HOLD 반환."""
        raise NotImplementedError


class MovingAverageCrossStrategy(Strategy):
    """단기/장기 이동평균 교차 추세추종. 골든크로스 매수, 데드크로스 매도."""

    def __init__(self, short: int = 5, long: int = 20):
        if short >= long:
            raise ValueError("short 는 long 보다 작아야 함")
        self.short = short
        self.long = long
        self.name = f"ma-cross-{short}-{long}"

    def decide(self, history: list[Bar], in_position: bool) -> str:
        if len(history) < self.long + 1:
            return HOLD
        closes = [b.close for b in history]
        short_now = statistics.fmean(closes[-self.short:])
        long_now = statistics.fmean(closes[-self.long:])
        short_prev = statistics.fmean(closes[-self.short - 1:-1])
        long_prev = statistics.fmean(closes[-self.long - 1:-1])
        crossed_up = short_prev <= long_prev and short_now > long_now
        crossed_down = short_prev >= long_prev and short_now < long_now
        if not in_position and crossed_up:
            return BUY
        if in_position and crossed_down:
            return SELL
        return HOLD


class MeanReversionStrategy(Strategy):
    """평균회귀: z-score 가 -z_entry 미만이면 매수, 0 이상으로 회복하면 매도."""

    def __init__(self, window: int = 20, z_entry: float = 1.5):
        self.window = window
        self.z_entry = z_entry
        self.name = f"mean-reversion-{window}-{z_entry}"

    def decide(self, history: list[Bar], in_position: bool) -> str:
        if len(history) < self.window:
            return HOLD
        closes = [b.close for b in history[-self.window:]]
        mean = statistics.fmean(closes)
        sd = statistics.pstdev(closes)
        if sd == 0:
            return HOLD
        z = (closes[-1] - mean) / sd
        if not in_position and z < -self.z_entry:
            return BUY
        if in_position and z >= 0:
            return SELL
        return HOLD


def build(name: str) -> Strategy:
    """이름으로 전략 생성(CLI 용)."""
    if name == "ma":
        return MovingAverageCrossStrategy()
    if name == "meanrev":
        return MeanReversionStrategy()
    raise ValueError(f"알 수 없는 전략: {name}")
