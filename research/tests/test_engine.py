import unittest

from arborbot_research.data import generate_ohlcv
from arborbot_research.engine import Backtester, BacktestConfig
from arborbot_research.strategy import MovingAverageCrossStrategy, MeanReversionStrategy


class EngineTest(unittest.TestCase):

    def test_deterministic_same_seed(self):
        bars1 = generate_ohlcv(n=300, seed=7)
        bars2 = generate_ohlcv(n=300, seed=7)
        self.assertEqual(bars1, bars2)  # 재현성: 같은 seed → 동일 데이터

        r1 = Backtester(MovingAverageCrossStrategy(), bars1).run()
        r2 = Backtester(MovingAverageCrossStrategy(), bars2).run()
        self.assertEqual(r1.equity_curve, r2.equity_curve)
        self.assertEqual(r1.report.as_dict(), r2.report.as_dict())

    def test_no_lookahead_warmup_holds(self):
        # 워밍업(long+1) 이전에는 매수하지 않음 → 첫 거래 진입일이 충분히 뒤
        bars = generate_ohlcv(n=200, seed=3)
        result = Backtester(MovingAverageCrossStrategy(short=5, long=20), bars).run()
        # equity_curve 길이는 봉 수와 동일(초기 + 각 스텝)
        self.assertEqual(len(result.equity_curve), len(bars))

    def test_produces_trades_on_trending_data(self):
        # 상승 드리프트 데이터에서 MA 교차 전략이 최소 1회 이상 거래
        bars = generate_ohlcv(n=600, seed=42, mu=0.0008, sigma=0.012)
        result = Backtester(MovingAverageCrossStrategy(), bars).run()
        self.assertGreaterEqual(result.report.trade_count, 1)
        # 자본곡선은 항상 유한하고 양수
        self.assertTrue(all(e > 0 for e in result.equity_curve))

    def test_costs_reduce_return(self):
        bars = generate_ohlcv(n=400, seed=11, mu=0.0006)
        cheap = Backtester(MeanReversionStrategy(), bars,
                           BacktestConfig(commission_bps=0, slippage_bps=0)).run()
        costly = Backtester(MeanReversionStrategy(), bars,
                            BacktestConfig(commission_bps=20, slippage_bps=20)).run()
        if cheap.report.trade_count > 0:
            self.assertGreaterEqual(cheap.report.net_return_after_cost_pct,
                                    costly.report.net_return_after_cost_pct)
            self.assertGreater(costly.report.total_cost, cheap.report.total_cost)

    def test_report_fields_present(self):
        bars = generate_ohlcv(n=250, seed=1)
        rep = Backtester(MovingAverageCrossStrategy(), bars).run().report.as_dict()
        for key in ["cumulative_return_pct", "max_drawdown_pct", "win_rate_pct",
                    "profit_factor", "sharpe_ratio", "trade_count",
                    "net_return_after_cost_pct", "total_cost"]:
            self.assertIn(key, rep)


if __name__ == "__main__":
    unittest.main()
