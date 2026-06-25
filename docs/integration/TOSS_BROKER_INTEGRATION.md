# 증권사(Broker) 실 연동 가이드

본 문서는 Mock 브로커를 **실 증권사 REST API**로 교체하는 방법을 설명합니다.

> ⚠️ **중요 — 토스증권 API 현황**
> 토스증권은 현재(2026년 기준) 개인용 주식 **주문 Open API**를 공식 문서로 공개하지 않습니다.
> 앱 내부 API를 리버스엔지니어링한 비공식 라이브러리는 **약관 위반·불안정·실거래 위험**이 크므로
> 자동매매에 사용하지 마십시오. 공식 트레이딩 API를 제공하는 증권사(예: 한국투자증권 KIS Developers,
> 키움증권)는 본 어댑터의 **설정값만 채워** 동일하게 연동할 수 있습니다.
>
> **이 프로젝트는 특정 증권사의 실제 스펙을 코드에 추측해 하드코딩하지 않습니다(절대 규칙 6).**
> 엔드포인트/인증/필드명은 모두 설정(`.env`/`application.yml`)으로 주입합니다.

---

## 1. 구조

- 포트: `BrokerPort`(application) — 유일한 증권사 호출 추상화.
- 구현 2종(설정으로 택1, 기본 `mock`):
  - `MockTossBrokerAdapter` — `trading.broker.provider=mock`(기본). 시뮬레이션.
  - `RestBrokerAdapter` — `trading.broker.provider=rest`. 실 HTTP 연동.
- 설정: `BrokerProperties`(`trading.broker.*`). 타임아웃/인증/경로/요청·응답 필드명을 모두 주입.
- 안전장치: RestClient 요청 타임아웃 + Resilience4j(`@Retry`/`@CircuitBreaker`) + 멱등키 헤더 + 비밀정보 로그 마스킹.

---

## 2. 활성화 절차

1. **공식 문서에서 스펙 확인** — 인증(토큰 발급) 방식, 주문/취소/조회 엔드포인트, 요청·응답 필드명, 성공 코드.
2. `.env` 설정(비밀값은 로컬/Secret Manager 에만):
   ```bash
   BROKER_PROVIDER=rest
   BROKER_API_BASE_URL=https://<증권사 API 호스트>
   BROKER_TOKEN_URL=https://<토큰 발급 URL>
   BROKER_APP_KEY=__로컬에서만__
   BROKER_APP_SECRET=__로컬에서만__
   BROKER_ACCOUNT_NO=__로컬에서만__
   ```
3. 필드명이 기본값과 다르면 `application.yml`에서 override:
   ```yaml
   trading:
     broker:
       paths:    { order: /v1/orders, cancel: /v1/orders/cancel, query: /v1/orders }
       request:  { symbol: symbol, side: side, quantity: quantity, price: price, order-type: orderType, account: accountNo }
       response: { order-ref: orderId, filled-quantity: filledQuantity, avg-price: avgPrice, result-code: resultCode, success-code: "0" }
       token:    { grant-type-key: grant_type, grant-type-value: client_credentials, app-key-key: appkey, app-secret-key: appsecret, access-token-key: access_token, expires-in-key: expires_in }
   ```
4. **반드시 단계적으로**: `dry-run=true` + `mode=PAPER` 또는 증권사 **모의투자(샌드박스)** 도메인으로 먼저 검증 →
   소액 `SEMI_AUTO`(사람 승인) → 소액 `AUTO` 순. (README 단계적 활성화 참조)

---

## 3. 매핑 지점 (공식 스펙 확인 후 채울 것)

| 항목 | 설정 키 | 비고 |
| --- | --- | --- |
| 토큰 발급 | `trading.broker.token-url`, `token.*` | OAuth2 client_credentials 가정(다르면 키 이름 조정) |
| 주문 | `paths.order`, `request.*` | 요청 본문 키 이름 |
| 응답 해석 | `response.*`, `response.success-code` | 주문번호/체결수량/평균가/결과코드 |
| 취소 | `paths.cancel` | |
| 조회(재동기화) | `paths.query` | 타임아웃 결과불명 복구·부분체결 폴링에 사용 |
| 멱등키 헤더 | `idempotency-header` | 중복 주문 방지 |

> 인증 방식이 OAuth2 client_credentials 가 아니거나(예: 서명/HMAC, 별도 해시 헤더), 응답이 중첩 JSON 이면
> `RestBrokerAdapter`의 `token()`/`buildOrderBody()`/`parseOrderResponse()`만 수정하면 됩니다(한 파일).

---

## 4. 검증

- 어댑터 단위 테스트: `RestBrokerAdapterTest`(MockRestServiceServer 로 실제 호출 없이 토큰/헤더/본문/응답·4xx 처리 검증).
- 실 연동 전 **증권사 모의투자 도메인**으로 동일 플로우(POST `/api/v1/signals` → `/api/v1/orders` → 승인)를 점검.
- 잔고/체결 조회 기반 재동기화(`/api/v1/batch/sync-orders`)가 정상 동작하는지 확인.

---

## 5. 보안 체크리스트

- [ ] `appKey/appSecret/accountNo/토큰`이 코드·로그·Git에 없음(`.env`는 추적 제외, 로그는 마스킹).
- [ ] 토큰은 메모리 캐시(만료 전 회전)만 사용, 파일/DB 평문 저장 금지.
- [ ] 실 연동은 `order-service` 계층에만 자격증명 노출(권한 분리).
- [ ] 최초 실거래는 소액·반자동·Kill Switch 점검 후.
