package com.tossai.trading.domain.account;

/**
 * 계좌. 계좌번호 등 비밀정보는 도메인/로그에 평문 보관하지 않는다.
 * accountRef 는 내부 참조용 식별자이며 실제 증권사 계좌번호가 아니다.
 */
public record Account(
        String accountId,
        String accountRef,
        boolean autoTradingEnabled
) {
}
