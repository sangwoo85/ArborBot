# API_CONTRACT.md — API 계약

본 문서는 **내부 서비스 간 API 계약과 핵심 DTO 스키마**를 정의합니다.
실제 OpenAPI(Swagger) 명세는 각 서비스가 런타임에 노출하며, 본 문서는 합의된 계약의 기준입니다.

> ⚠️ **토스증권 Open API의 실제 엔드포인트·필드·인증 방식은 공식 문서 확인 전까지 추측하지 않습니다.**
> 본 문서의 broker 관련 부분은 **내부 어댑터 인터페이스(추상화)** 만 정의하며, 외부 스펙은 별도 확정합니다.

---

## 1. 공통 규약

- 프로토콜: HTTP/JSON, 시각은 ISO-8601(`+09:00` 또는 UTC) 사용.
- 금액 단위: 원(KRW), 정수. 비율: `0.0~1.0`.
- 식별자: UUID(문자열).
- 오류 응답 공통 형식:

```json
{
  "timestamp": "2026-06-25T09:35:00+09:00",
  "code": "RISK_REJECTED",
  "message": "사람이 읽을 수 있는 메시지(비밀정보 미포함)",
  "details": [ { "field": "positionPct", "reason": "EXCEEDS_MAX" } ]
}
```

- 응답에는 **비밀정보/계좌번호/토큰을 절대 포함하지 않는다.**

---

## 2. 핵심 도메인 DTO

### 2.1 TradingSignal (AI 산출물)

```json
{
  "signalId": "uuid",
  "symbol": "005930",
  "decision": "BUY | SELL | HOLD",
  "confidenceScore": 0.74,
  "recommendedPositionSize": 0.05,
  "entryPriceRange": { "min": 71000, "max": 72500 },
  "stopLossPrice": 68500,
  "takeProfitPrice": 78000,
  "holdingPeriod": "INTRADAY | SWING | POSITION",
  "validUntil": "2026-06-25T15:20:00+09:00",
  "marketRegime": "BULL | BEAR | SIDEWAYS | HIGH_VOLATILITY | EVENT",
  "strategyId": "momentum-v3",
  "rationale": "string",
  "inputSnapshotRef": "uuid",
  "createdAt": "2026-06-25T09:35:00+09:00"
}
```

### 2.2 OrderCandidate (신호 → 주문 후보)

```json
{
  "candidateId": "uuid",
  "signalId": "uuid",
  "symbol": "005930",
  "side": "BUY | SELL",
  "quantity": 10,
  "orderType": "LIMIT | MARKET",
  "limitPrice": 72000,
  "requestedAt": "2026-06-25T09:36:00+09:00"
}
```

### 2.3 RiskEvaluation (위험 검증 결과)

```json
{
  "evaluationId": "uuid",
  "candidateId": "uuid",
  "result": "ALLOW | REJECT",
  "violations": [
    { "rule": "MAX_POSITION_PCT", "limit": 0.10, "actual": 0.13 }
  ],
  "killSwitch": "OFF | GLOBAL | STRATEGY | SYMBOL",
  "evaluatedAt": "2026-06-25T09:36:01+09:00"
}
```

`violations` 가능한 `rule` 값(요약):
`MAX_POSITION_PCT, MAX_SECTOR_PCT, MAX_ORDER_AMOUNT, MAX_DAILY_ORDER_AMOUNT,
MAX_DAILY_LOSS, MAX_LOSS_PCT, DUPLICATE_ORDER, RATE_LIMIT, INSUFFICIENT_BALANCE,
MARKET_CLOSED, HALTED_SYMBOL, ILLIQUID_OR_VOLATILE, SIGNAL_EXPIRED, LOW_CONFIDENCE, KILL_SWITCH`

### 2.4 Order / OrderExecution (실제 주문·체결, 신호와 분리 저장)

```json
{
  "orderId": "uuid",
  "candidateId": "uuid",
  "symbol": "005930",
  "side": "BUY",
  "quantity": 10,
  "orderType": "LIMIT",
  "limitPrice": 72000,
  "mode": "PAPER | SEMI_AUTO | AUTO",
  "status": "PENDING_APPROVAL | SENT | FILLED | PARTIALLY_FILLED | REJECTED | CANCELLED",
  "execution": {
    "filledQuantity": 10,
    "avgFillPrice": 71950,
    "brokerOrderRef": "string-or-null",
    "executedAt": "2026-06-25T09:36:05+09:00"
  }
}
```

---

## 3. 내부 서비스 엔드포인트 (계약 기준, 예시)

> 경로/포트는 게이트웨이 라우팅에 따름. 모든 변경 작업은 인증 필요.

### ai-trader-service
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/internal/signals/generate` | 입력 컨텍스트로 신호 생성 |
| `GET` | `/internal/signals/{signalId}` | 신호 조회 |
| `GET` | `/internal/signals?symbol=&from=&to=` | 신호 목록 |

→ **주문 관련 엔드포인트 없음(설계상 부재).**

### risk-engine
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/internal/risk/evaluate` | OrderCandidate 검증 → RiskEvaluation |
| `GET` | `/internal/risk/limits` | 현재 한도/사용량 조회 |

### order-service
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/internal/orders` | 검증 통과 후보를 주문으로 등록(모드별 처리) |
| `POST` | `/internal/orders/{orderId}/approve` | SEMI_AUTO 사람 승인 |
| `POST` | `/internal/orders/{orderId}/cancel` | 주문 취소 |
| `GET` | `/internal/orders/{orderId}` | 주문/체결 조회 |

### portfolio-service
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/internal/portfolio` | 포지션/현금/섹터 비중 |
| `GET` | `/internal/portfolio/pnl?date=` | 일일 손익 |

### 관리자(admin, 게이트웨이)
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/admin/kill-switch` | Kill Switch ON/OFF (사유 필수, 감사 기록) |
| `PUT` | `/admin/risk/limits` | 한도 변경 (승인·감사 필요) |
| `PUT` | `/admin/trading-mode` | 거래 모드 전환 |

---

## 4. Broker 어댑터 인터페이스 (외부 스펙 추상화)

증권사 호출은 **order-service 내부 어댑터**로만 추상화한다. 실제 필드는 공식 문서 확정 후 매핑한다.

```text
interface BrokerClient {
  BrokerOrderResult placeOrder(BrokerOrderRequest req);   // 매수/매도 주문
  BrokerOrderResult cancelOrder(String brokerOrderRef);
  Balance getBalance();
  Position[] getPositions();
}
```

- 인증 토큰 주입·회전은 어댑터 내부에서 처리하며 외부로 노출하지 않는다.
- 응답은 내부 DTO로 변환하고, 비밀/민감 필드는 저장·로그에서 제외한다.

---

## 5. 멱등성 / 동시성

- 주문 생성은 **멱등키**(candidateId 기반)로 중복 실행을 방지한다.
- 중복/빈도 제한은 Redis로 판정한다([RISK_POLICY.md](RISK_POLICY.md)).

---

## 6. 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md) · [AI_TRADER_POLICY.md](AI_TRADER_POLICY.md) · [RISK_POLICY.md](RISK_POLICY.md)
