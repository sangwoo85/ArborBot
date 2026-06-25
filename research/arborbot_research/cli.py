"""백테스트 CLI.

예) python -m arborbot_research.cli --strategy ma --bars 500 --seed 42
"""
from __future__ import annotations

import argparse
import json

from . import strategy as strategy_mod
from .data import generate_ohlcv, load_csv
from .engine import Backtester, BacktestConfig


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description="ArborBot 백테스트")
    p.add_argument("--strategy", default="ma", choices=["ma", "meanrev"])
    p.add_argument("--bars", type=int, default=500)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--csv", default=None, help="실데이터 CSV 경로(주면 합성 데이터 대신 사용)")
    p.add_argument("--cash", type=float, default=10_000_000.0)
    p.add_argument("--commission-bps", type=float, default=5.0)
    p.add_argument("--slippage-bps", type=float, default=5.0)
    args = p.parse_args(argv)

    bars = load_csv(args.csv) if args.csv else generate_ohlcv(n=args.bars, seed=args.seed)
    strat = strategy_mod.build(args.strategy)
    cfg = BacktestConfig(initial_cash=args.cash,
                         commission_bps=args.commission_bps,
                         slippage_bps=args.slippage_bps)
    result = Backtester(strat, bars, cfg).run()

    print(json.dumps(result.report.as_dict(), ensure_ascii=False, indent=2))
    print(f"# bars={len(bars)} trades={len(result.trades)} "
          f"final_equity={result.equity_curve[-1]:,.0f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
