"""백테스트 엔진. 봉을 순차 재생하며 전략 신호를 받아 체결을 시뮬레이션한다.

원칙:
- 룩어헤드 금지: 봉 i 종가로 판단하고 봉 i+1 시가에 체결.
- 거래비용 반영: 진입/청산마다 수수료(bps)와 슬리피지(bps)를 차감.
- 롱 온리, 1종목, 단순 포지션(자본의 일정 비율). 손절/익절 옵션.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from . import metrics
from .data import Bar
from .strategy import BUY, SELL, Strategy


@dataclass(frozen=True)
class Trade:
    entry_date: str
    exit_date: str
    entry_price: float
    exit_price: float
    quantity: int
    pnl: float
    cost: float
    return_pct: float


@dataclass
class BacktestConfig:
    initial_cash: float = 10_000_000.0
    position_fraction: float = 0.95   # 진입 시 사용할 자본 비율
    commission_bps: float = 5.0        # 편도 수수료(0.05%)
    slippage_bps: float = 5.0          # 편도 슬리피지
    stop_loss_pct: float = 0.05        # 손절 -5%
    take_profit_pct: float = 0.15      # 익절 +15%


@dataclass
class BacktestResult:
    equity_curve: list[float] = field(default_factory=list)
    trades: list[Trade] = field(default_factory=list)
    report: metrics.PerformanceReport | None = None


class Backtester:
    def __init__(self, strategy: Strategy, bars: list[Bar], config: BacktestConfig | None = None):
        self.strategy = strategy
        self.bars = bars
        self.cfg = config or BacktestConfig()

    def _apply_slippage(self, price: float, side: str) -> float:
        adj = self.cfg.slippage_bps / 10_000.0
        return price * (1 + adj) if side == "buy" else price * (1 - adj)

    def _commission(self, notional: float) -> float:
        return abs(notional) * self.cfg.commission_bps / 10_000.0

    def run(self) -> BacktestResult:
        cash = self.cfg.initial_cash
        qty = 0
        entry_price = 0.0
        entry_date = ""
        entry_cost = 0.0
        total_cost = 0.0
        trades: list[Trade] = []
        equity_curve: list[float] = [cash]

        n = len(self.bars)
        for i in range(n - 1):
            history = self.bars[: i + 1]
            nxt = self.bars[i + 1]
            in_position = qty > 0

            exit_now = False
            if in_position:
                # 손절/익절 우선 점검(다음 봉 시가 기준)
                if nxt.open <= entry_price * (1 - self.cfg.stop_loss_pct):
                    exit_now = True
                elif nxt.open >= entry_price * (1 + self.cfg.take_profit_pct):
                    exit_now = True

            decision = self.strategy.decide(history, in_position)

            if not in_position and decision == BUY:
                fill = self._apply_slippage(nxt.open, "buy")
                budget = cash * self.cfg.position_fraction
                q = int(budget // fill)
                if q > 0:
                    notional = q * fill
                    comm = self._commission(notional)
                    cash -= notional + comm
                    qty = q
                    entry_price = fill
                    entry_date = nxt.date
                    entry_cost = comm
                    total_cost += comm
            elif in_position and (decision == SELL or exit_now):
                fill = self._apply_slippage(nxt.open, "sell")
                notional = qty * fill
                comm = self._commission(notional)
                cash += notional - comm
                total_cost += comm
                pnl = (fill - entry_price) * qty - (entry_cost + comm)
                ret = (fill - entry_price) / entry_price
                trades.append(Trade(entry_date, nxt.date, entry_price, fill, qty,
                                    round(pnl, 2), round(entry_cost + comm, 2), ret))
                qty = 0
                entry_price = 0.0

            # 자본 평가(현금 + 보유 평가, 당일 종가)
            equity_curve.append(cash + qty * self.bars[i + 1].close)

        # 종료 시 잔여 포지션 청산(마지막 종가)
        if qty > 0:
            last = self.bars[-1]
            fill = self._apply_slippage(last.close, "sell")
            notional = qty * fill
            comm = self._commission(notional)
            cash += notional - comm
            total_cost += comm
            pnl = (fill - entry_price) * qty - (entry_cost + comm)
            trades.append(Trade(entry_date, last.date, entry_price, fill, qty,
                                round(pnl, 2), round(entry_cost + comm, 2),
                                (fill - entry_price) / entry_price))
            qty = 0
            equity_curve[-1] = cash

        report = self._report(equity_curve, trades, total_cost)
        return BacktestResult(equity_curve=equity_curve, trades=trades, report=report)

    def _report(self, equity: list[float], trades: list[Trade], total_cost: float) -> metrics.PerformanceReport:
        win_rate, profit_factor = metrics.trade_stats([t.pnl for t in trades])
        rets = metrics.returns_from_equity(equity)
        return metrics.PerformanceReport(
            strategy=self.strategy.name,
            cumulative_return_pct=100.0 * metrics.cumulative_return(equity),
            max_drawdown_pct=100.0 * metrics.max_drawdown(equity),
            win_rate_pct=win_rate,
            profit_factor=profit_factor,
            sharpe_ratio=metrics.sharpe_ratio(rets),
            trade_count=len(trades),
            net_return_after_cost_pct=100.0 * metrics.cumulative_return(equity),
            total_cost=total_cost,
        )
