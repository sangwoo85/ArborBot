-- 종목 마스터/상태(가상). 실제 종목 추천 정보가 아니다.
CREATE TABLE instrument (
    symbol     VARCHAR(32) PRIMARY KEY,
    sector     VARCHAR(48),
    last_price DECIMAL(19,4),
    tradable   BOOLEAN,
    halted     BOOLEAN,
    illiquid   BOOLEAN
);

-- 정상 거래 종목
INSERT INTO instrument (symbol, sector, last_price, tradable, halted, illiquid)
VALUES ('005930', 'SEMICONDUCTOR', 70000.0000, TRUE, FALSE, FALSE);
INSERT INTO instrument (symbol, sector, last_price, tradable, halted, illiquid)
VALUES ('000660', 'SEMICONDUCTOR', 130000.0000, TRUE, FALSE, FALSE);
-- 거래정지 종목(매수 차단 대상)
INSERT INTO instrument (symbol, sector, last_price, tradable, halted, illiquid)
VALUES ('900110', 'MISC', 1500.0000, FALSE, TRUE, FALSE);
-- 유동성 부족 종목(매수 차단 대상)
INSERT INTO instrument (symbol, sector, last_price, tradable, halted, illiquid)
VALUES ('900120', 'MISC', 800.0000, TRUE, FALSE, TRUE);
