import math
import unittest

from arborbot_research import metrics


class MetricsTest(unittest.TestCase):

    def test_cumulative_return(self):
        self.assertAlmostEqual(metrics.cumulative_return([100, 110]), 0.10)
        self.assertEqual(metrics.cumulative_return([100]), 0.0)

    def test_max_drawdown(self):
        # 고점 120 → 저점 90 = 25% 낙폭
        self.assertAlmostEqual(metrics.max_drawdown([100, 120, 90, 130]), 0.25)
        self.assertAlmostEqual(metrics.max_drawdown([100, 100, 100]), 0.0)

    def test_sharpe_zero_when_no_variation(self):
        self.assertEqual(metrics.sharpe_ratio([0.01, 0.01, 0.01]), 0.0)
        self.assertEqual(metrics.sharpe_ratio([0.01]), 0.0)

    def test_sharpe_positive_for_positive_drift(self):
        rets = [0.01, 0.02, 0.005, 0.015, 0.01]
        self.assertGreater(metrics.sharpe_ratio(rets), 0.0)

    def test_trade_stats(self):
        win_rate, pf = metrics.trade_stats([10, -5, 20, -5])
        self.assertAlmostEqual(win_rate, 50.0)
        self.assertAlmostEqual(pf, 30 / 10)

    def test_trade_stats_empty(self):
        self.assertEqual(metrics.trade_stats([]), (0.0, 0.0))

    def test_returns_from_equity(self):
        rets = metrics.returns_from_equity([100, 110, 99])
        self.assertAlmostEqual(rets[0], 0.10)
        self.assertAlmostEqual(rets[1], -0.10)


if __name__ == "__main__":
    unittest.main()
