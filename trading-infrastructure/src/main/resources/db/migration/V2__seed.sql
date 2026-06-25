-- 데모/실행용 시드 데이터. 실제 계좌/종목 추천 정보가 아니다(가상 데이터).

-- 전역 Kill Switch: OFF
INSERT INTO kill_switch (switch_key, scope, target, enabled, reason, actor, updated_at)
VALUES ('GLOBAL|', 'GLOBAL', NULL, FALSE, 'init', 'system', CURRENT_TIMESTAMP);

-- 계좌 잔고(가상)
INSERT INTO account_balance (account_id, cash, orderable_amount)
VALUES ('ACC-DEMO', 10000000.0000, 10000000.0000);

-- 보유 포지션(가상)
INSERT INTO positions (symbol, sector, quantity, sellable_quantity, avg_price, last_price, evaluation_amount, unrealized_pnl)
VALUES ('000660', 'SEMICONDUCTOR', 10, 10, 120000.0000, 130000.0000, 1300000.0000, 100000.0000);
-- 평균가(80000)가 현재가(70000)보다 높은 포지션 → 매도 시 실현 손실 발생(손실 한도 테스트용)
INSERT INTO positions (symbol, sector, quantity, sellable_quantity, avg_price, last_price, evaluation_amount, unrealized_pnl)
VALUES ('005930', 'SEMICONDUCTOR', 5, 5, 80000.0000, 70000.0000, 350000.0000, -50000.0000);

-- 전략(승인된 풀)
INSERT INTO trading_strategy (strategy_id, version, description, target_regime, active, auto_trading_eligible)
VALUES ('momentum-v1', 'v1', '추세추종 전략', 'BULL', TRUE, TRUE);
INSERT INTO trading_strategy (strategy_id, version, description, target_regime, active, auto_trading_eligible)
VALUES ('mean-reversion-v1', 'v1', '평균회귀 전략', 'SIDEWAYS', TRUE, TRUE);
INSERT INTO trading_strategy (strategy_id, version, description, target_regime, active, auto_trading_eligible)
VALUES ('experimental-v0', 'v0', '실험 전략(자동주문 비적격)', 'EVENT', TRUE, FALSE);

-- 전략 성과(거래비용 반영 수익률 포함)
-- momentum-v1: 양호 -> 자동주문 적격
INSERT INTO strategy_performance
(strategy_id, cumulative_return_percent, max_drawdown_percent, win_rate_percent, profit_factor, sharpe_ratio, trade_count, net_return_after_cost_percent, consecutive_losses)
VALUES ('momentum-v1', 18.5000, 8.0000, 56.0000, 1.6000, 1.2000, 120, 15.2000, 1);
-- mean-reversion-v1: MDD 과도 -> 자동주문 부적격
INSERT INTO strategy_performance
(strategy_id, cumulative_return_percent, max_drawdown_percent, win_rate_percent, profit_factor, sharpe_ratio, trade_count, net_return_after_cost_percent, consecutive_losses)
VALUES ('mean-reversion-v1', 5.0000, 25.0000, 48.0000, 1.0500, 0.3000, 60, 2.1000, 6);
-- experimental-v0: 표본 부족 -> 부적격
INSERT INTO strategy_performance
(strategy_id, cumulative_return_percent, max_drawdown_percent, win_rate_percent, profit_factor, sharpe_ratio, trade_count, net_return_after_cost_percent, consecutive_losses)
VALUES ('experimental-v0', 2.0000, 5.0000, 50.0000, 1.0000, 0.6000, 5, 1.0000, 0);
