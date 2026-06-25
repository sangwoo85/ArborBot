-- T+2 매수 결제 로트. 매수 체결 시 보유수량은 즉시 늘지만 매도가능수량은 결제일 이후 반영된다.
CREATE TABLE settlement_lot (
    lot_id      VARCHAR(64) PRIMARY KEY,
    symbol      VARCHAR(32),
    quantity    BIGINT,
    settle_date DATE,
    settled     BOOLEAN
);
CREATE INDEX idx_settle_due ON settlement_lot (settled, settle_date);
