package com.tossai.trading.common.util;

/**
 * 로그/응답에서 계좌번호, 토큰, 키 등 비밀정보를 마스킹한다.
 * (SECURITY 정책: 비밀정보는 코드/로그/응답에 노출하지 않는다.)
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    /** 계좌번호: 뒤 4자리만 노출. */
    public static String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return "****";
        }
        String digits = accountNo.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "****-**-" + last4;
    }

    /** 토큰/키: 앞 4자만 노출하고 나머지는 가린다. */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "****";
        }
        if (token.length() <= 4) {
            return "****";
        }
        return token.substring(0, 4) + "****";
    }
}
