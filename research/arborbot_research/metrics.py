"""성과 지표. STRATEGY_GOVERNANCE 의 지표를 거래비용 반영 기준으로 계산한다."""
from __future__ import annotations

import math
import statistics
from dataclasses import dataclass


def returns_from_equity(equity: list[float]) -> list[float]:
    """자본곡선 → 기간 수익률 시퀀스."""
    out: list[float] = []
    for i in range(1, len(equity)):
        prev = equity[i - 1]
        out.append((equity[i] - prev) / prev if prev != 0 else 0.0)
    return out


def cumulative_return(equity: list[float]) -> float:
    if len(equity) < 2 or equity[0] == 0:
        return 0.0
    return equity[-1] / equity[0] - 1.0


def max_drawdown(equity: list[float]) -> float:
    """고점 대비 최대 낙폭(양수 비율). 예: 0.25 == -25%."""
    peak = float("-inf")
    mdd = 0.0
    for v in equity:
        peak = max(peak, v)
        if peak > 0:
            mdd = max(mdd, (peak - v) / peak)
    return mdd


def sharpe_ratio(returns: list[float], periods_per_year: int = 252, rf: float = 0.0) -> float:
    """연율화 샤프. 표본 부족/무변동 시 0."""
    if len(returns) < 2:
        return 0.0
    excess = [r - rf / periods_per_year for r in returns]
    sd = statistics.stdev(excess)
    if sd == 0:
        return 0.0
    return (statistics.fmean(excess) / sd) * math.sqrt(periods_per_year)


@dataclass(frozen=True)
class PerformanceReport:
    strategy: str
    cumulative_return_pct: float
    max_drawdown_pct: float
    win_rate_pct: float
    profit_factor: float
    sharpe_ratio: float
    trade_count: int
    net_return_after_cost_pct: float   # 거래비용 반영(자본곡선 기준)
    total_cost: float

    def as_dict(self) -> dict:
        return {
            "strategy": self.strategy,
            "cumulative_return_pct": round(self.cumulative_return_pct, 4),
            "max_drawdown_pct": round(self.max_drawdown_pct, 4),
            "win_rate_pct": round(self.win_rate_pct, 4),
            "profit_factor": round(self.profit_factor, 4),
            "sharpe_ratio": round(self.sharpe_ratio, 4),
            "trade_count": self.trade_count,
            "net_return_after_cost_pct": round(self.net_return_after_cost_pct, 4),
            "total_cost": round(self.total_cost, 2),
        }


def trade_stats(trade_pnls: list[float]) -> tuple[float, float]:
    """거래별 손익 리스트 → (승률%, 손익비). 손익비=총이익/총손실."""
    if not trade_pnls:
        return 0.0, 0.0
    wins = [p for p in trade_pnls if p > 0]
    losses = [-p for p in trade_pnls if p < 0]
    win_rate = 100.0 * len(wins) / len(trade_pnls)
    gross_profit = sum(wins)
    gross_loss = sum(losses)
    if gross_loss == 0:
        profit_factor = float("inf") if gross_profit > 0 else 0.0
    else:
        profit_factor = gross_profit / gross_loss
    return win_rate, profit_factor
