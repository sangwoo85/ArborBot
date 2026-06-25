-- 초기 스키마. H2(MySQL 모드) 및 MySQL 8 호환을 위한 이식성 있는 DDL.
-- 컬럼명은 Spring 물리 네이밍 전략(camelCase -> snake_case)과 일치시킨다.

CREATE TABLE trading_signal (
    signal_id                          VARCHAR(64)   PRIMARY KEY,
    strategy_id                        VARCHAR(64),
    strategy_version                   VARCHAR(32),
    model_version                      VARCHAR(64),
    symbol                             VARCHAR(32),
    signal_type                        VARCHAR(8),
    confidence_score                   INT,
    recommended_position_size_percent  DECIMAL(9,4),
    entry_price_min                    DECIMAL(19,4),
    entry_price_max                    DECIMAL(19,4),
    stop_loss_price                    DECIMAL(19,4),
    take_profit_price                  DECIMAL(19,4),
    holding_period                     VARCHAR(32),
    valid_until                        TIMESTAMP,
    market_regime                      VARCHAR(24),
    rationale                          VARCHAR(2000),
    risk_flags                         VARCHAR(1000),
    created_at                         TIMESTAMP
);

CREATE TABLE orders (
    order_id              VARCHAR(64)   PRIMARY KEY,
    correlation_id        VARCHAR(64),
    strategy_signal_id    VARCHAR(64),
    portfolio_decision_id VARCHAR(64),
    symbol                VARCHAR(32),
    side                  VARCHAR(8),
    order_type            VARCHAR(8),
    quantity              BIGINT,
    limit_price           DECIMAL(19,4),
    idempotency_key       VARCHAR(80),
    order_mode            VARCHAR(16),
    dry_run               BOOLEAN,
    status                VARCHAR(24),
    filled_quantity       BIGINT,
    reject_reason         VARCHAR(500),
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP
);
CREATE INDEX idx_orders_idem ON orders (idempotency_key);
CREATE INDEX idx_orders_created ON orders (created_at);

CREATE TABLE order_execution (
    execution_id     VARCHAR(64)   PRIMARY KEY,
    order_id         VARCHAR(64),
    correlation_id   VARCHAR(64),
    filled_quantity  BIGINT,
    avg_fill_price   DECIMAL(19,4),
    fee              DECIMAL(19,4),
    tax              DECIMAL(19,4),
    broker_order_ref VARCHAR(80),
    executed_at      TIMESTAMP
);
CREATE INDEX idx_exec_order ON order_execution (order_id);

CREATE TABLE kill_switch (
    switch_key VARCHAR(128) PRIMARY KEY,
    scope      VARCHAR(16),
    target     VARCHAR(64),
    enabled    BOOLEAN,
    reason     VARCHAR(500),
    actor      VARCHAR(64),
    updated_at TIMESTAMP
);

CREATE TABLE audit_log (
    audit_id       VARCHAR(64) PRIMARY KEY,
    correlation_id VARCHAR(64),
    category       VARCHAR(24),
    action         VARCHAR(48),
    detail         VARCHAR(1000),
    actor          VARCHAR(64),
    occurred_at    TIMESTAMP
);

CREATE TABLE trading_strategy (
    strategy_id           VARCHAR(64) PRIMARY KEY,
    version               VARCHAR(32),
    description           VARCHAR(500),
    target_regime         VARCHAR(24),
    active                BOOLEAN,
    auto_trading_eligible BOOLEAN
);

CREATE TABLE strategy_performance (
    strategy_id                    VARCHAR(64) PRIMARY KEY,
    cumulative_return_percent      DECIMAL(9,4),
    max_drawdown_percent           DECIMAL(9,4),
    win_rate_percent               DECIMAL(9,4),
    profit_factor                  DECIMAL(9,4),
    sharpe_ratio                   DECIMAL(9,4),
    trade_count                    INT,
    net_return_after_cost_percent  DECIMAL(9,4),
    consecutive_losses             INT
);

CREATE TABLE positions (
    symbol            VARCHAR(32) PRIMARY KEY,
    sector            VARCHAR(48),
    quantity          BIGINT,
    sellable_quantity BIGINT,
    avg_price         DECIMAL(19,4),
    last_price        DECIMAL(19,4),
    evaluation_amount DECIMAL(19,4),
    unrealized_pnl    DECIMAL(19,4)
);

CREATE TABLE account_balance (
    account_id       VARCHAR(64) PRIMARY KEY,
    cash             DECIMAL(19,4),
    orderable_amount DECIMAL(19,4)
);

CREATE TABLE order_outbox (
    order_id        VARCHAR(64) PRIMARY KEY,
    idempotency_key VARCHAR(80),
    status          VARCHAR(16),
    attempts        INT,
    last_error      VARCHAR(500),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE daily_risk_usage (
    usage_date    DATE PRIMARY KEY,
    order_amount  DECIMAL(19,4),
    realized_loss DECIMAL(19,4)
);
