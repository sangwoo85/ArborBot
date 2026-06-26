"""백테스트 CLI.

예) python -m arborbot_research.cli --strategy ma --bars 500 --seed 42
"""
from __future__ import annotations

import argparse
import json
import urllib.request

from . import strategy as strategy_mod
from .data import generate_ohlcv, load_csv
from .engine import Backtester, BacktestConfig


def _to_submit_payload(report: dict) -> dict:
    """research 리포트 → strategy-engine 백테스트 제출 스키마(camelCase Pct)."""
    return {
        "cumulativeReturnPct": report["cumulative_return_pct"],
        "maxDrawdownPct": report["max_drawdown_pct"],
        "winRatePct": report["win_rate_pct"],
        "profitFactor": report["profit_factor"],
        "sharpeRatio": report["sharpe_ratio"],
        "tradeCount": report["trade_count"],
        "netReturnAfterCostPct": report["net_return_after_cost_pct"],
        "consecutiveLosses": report.get("consecutive_losses", 0),
    }


def submit_backtest(base_url: str, strategy_id: str, report: dict) -> dict:
    """백테스트 리포트를 Java strategy-engine 으로 제출(승격 판정 수신)."""
    url = f"{base_url.rstrip('/')}/api/v1/strategies/{strategy_id}/backtest"
    data = json.dumps(_to_submit_payload(report)).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description="ArborBot 백테스트")
    p.add_argument("--strategy", default="ma", choices=["ma", "meanrev"])
    p.add_argument("--bars", type=int, default=500)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--csv", default=None, help="실데이터 CSV 경로(주면 합성 데이터 대신 사용)")
    p.add_argument("--cash", type=float, default=10_000_000.0)
    p.add_argument("--commission-bps", type=float, default=5.0)
    p.add_argument("--slippage-bps", type=float, default=5.0)
    p.add_argument("--submit-url", default=None,
                   help="주면 백테스트 리포트를 strategy-engine 으로 제출(예: http://localhost:8080)")
    p.add_argument("--strategy-id", default=None,
                   help="제출 대상 전략 ID(--submit-url 과 함께)")
    args = p.parse_args(argv)

    bars = load_csv(args.csv) if args.csv else generate_ohlcv(n=args.bars, seed=args.seed)
    strat = strategy_mod.build(args.strategy)
    cfg = BacktestConfig(initial_cash=args.cash,
                         commission_bps=args.commission_bps,
                         slippage_bps=args.slippage_bps)
    result = Backtester(strat, bars, cfg).run()

    report = result.report.as_dict()
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(f"# bars={len(bars)} trades={len(result.trades)} "
          f"final_equity={result.equity_curve[-1]:,.0f}")

    if args.submit_url:
        strategy_id = args.strategy_id or strat.name
        try:
            promotion = submit_backtest(args.submit_url, strategy_id, report)
            print("# 승격 판정:", json.dumps(promotion, ensure_ascii=False))
        except Exception as e:  # 네트워크/서버 미가동 등은 백테스트 자체를 실패시키지 않음
            print(f"# 제출 실패(백테스트는 정상): {e}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
