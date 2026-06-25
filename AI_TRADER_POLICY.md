# AI_TRADER_POLICY.md — AI 트레이더 정책

본 문서는 AI 트레이더의 **입력 데이터, 출력 형식, 전략 선택 방식, 신뢰도 기준, 신호 만료 규칙**을 정의합니다.

> **불변 규칙**
> 1. AI는 증권사 주문 API를 직접 호출하지 않는다.
> 2. AI는 `TradingSignal`만 생성한다.
> 3. AI의 판단 근거와 입력 데이터 요약은 저장한다.
> 4. AI 신호와 실제 주문 결과는 분리 저장한다.

---

## 1. AI 트레이더의 역할

1. **시장 국면 분석** — 상승장 / 하락장 / 횡보장 / 고변동성장 / 이벤트 장세
2. **활성 전략 선택** — 여러 전략의 최근 성과를 비교해 현재 국면에 맞는 전략 선택
3. **종목별 신호 생성** — BUY / SELL / HOLD + 부가 메타데이터
4. **전략 비중 조정** — 성과 악화 전략을 비활성화하거나 비중 축소
5. **포트폴리오 위험 우선** — 개별 종목 수익보다 전체 위험을 우선 고려

---

## 2. 입력 데이터 (Input)

AI는 아래 데이터의 **정규화된 요약**을 입력으로 받습니다. (원천 데이터는 market-data-service가 제공)

| 범주 | 항목(예시) |
| --- | --- |
| 시장 데이터 | 가격(OHLCV), 거래량, 호가/유동성, 지수, 변동성(VIX류) |
| 기술 지표 | 이동평균, RSI, MACD, 볼린저밴드, ATR, 모멘텀 |
| 뉴스/이벤트 | 공시, 실적, 거시 이벤트, 뉴스 감성 점수(요약된 신호) |
| 전략 성과 | 전략별 최근 수익률·샤프·MDD·승률 |
| 포트폴리오 상태 | 현재 포지션, 종목/섹터 비중, 현금, 평가손익 |
| 위험 컨텍스트 | 한도 잔여치, Kill Switch 상태, 거래 가능 시간 |

### 2.1 입력 요약 저장(재현성)

- 신호 생성 시 사용한 입력의 **요약 스냅샷**을 `signal_input_snapshot`에 저장한다.
- 저장 목적: 사후 검증, 디버깅, 전략 평가, 재현.
- **개인 식별 정보·비밀정보(키/토큰/계좌번호)는 입력 요약에 포함하지 않는다.**

---

## 3. 출력 형식 (TradingSignal)

AI의 유일한 산출물은 `TradingSignal`입니다.

```json
{
  "signalId": "uuid",
  "symbol": "005930",
  "decision": "BUY",                 // BUY | SELL | HOLD
  "confidenceScore": 0.74,           // 0.0 ~ 1.0
  "recommendedPositionSize": 0.05,   // 포트폴리오 대비 권장 비중 (0~1)
  "entryPriceRange": { "min": 71000, "max": 72500 },
  "stopLossPrice": 68500,
  "takeProfitPrice": 78000,
  "holdingPeriod": "SWING",          // INTRADAY | SWING | POSITION 또는 기간(예: P5D)
  "validUntil": "2026-06-25T15:20:00+09:00",
  "marketRegime": "HIGH_VOLATILITY", // BULL | BEAR | SIDEWAYS | HIGH_VOLATILITY | EVENT
  "strategyId": "mean-reversion-v2",
  "rationale": "RSI 과매도 + 지지선 근접, 단기 반등 기대. 섹터 비중 여유 있음.",
  "inputSnapshotRef": "snapshot-uuid",
  "createdAt": "2026-06-25T09:35:00+09:00"
}
```

### 3.1 필드 규칙

| 필드 | 규칙 |
| --- | --- |
| `decision` | BUY/SELL/HOLD 외 값 불가 |
| `confidenceScore` | `[0,1]` 범위. 최소 신뢰도 미만이면 주문 후보에서 제외(아래 5장) |
| `recommendedPositionSize` | **권장값일 뿐**, 최종 비중은 Risk Engine 한도가 우선 |
| `entryPriceRange` | min ≤ max, 시장가 신호는 별도 플래그 |
| `stopLossPrice` / `takeProfitPrice` | BUY는 stop < entry < takeProfit 일관성 검증 |
| `validUntil` | 만료 시각(이후 무효, 아래 6장) |
| `rationale` | 사람이 읽을 수 있는 근거. **반드시 저장** |
| `inputSnapshotRef` | 입력 요약 스냅샷 참조 ID |

> **`recommendedPositionSize`는 권장치이며 강제력이 없다.** 실제 비중·실행 여부는
> 항상 [RISK_POLICY.md](RISK_POLICY.md)의 한도가 우선한다.

---

## 4. 전략 선택 방식

1. strategy-service가 **활성 후보 전략 목록**과 최근 성과를 제공한다.
2. AI는 현재 **시장 국면**과 각 전략의 적합도/최근 성과를 비교한다.
3. 국면별 가중치를 적용해 활성 전략을 선택한다(예: 횡보장→평균회귀, 추세장→모멘텀).
4. 성과가 악화된 전략은 신호 생성에서 제외하거나 비중을 축소한다.
5. 최종 전략 선택과 사유는 신호의 `strategyId`/`rationale`에 남긴다.

> 전략의 등록·승격·중단 자체는 [STRATEGY_GOVERNANCE.md](STRATEGY_GOVERNANCE.md)가 통제한다.
> AI는 **승인된 전략 풀 내에서만** 선택한다(임의의 새 전략을 즉흥 생성하지 않는다).

---

## 5. 신뢰도 기준

| 신뢰도 구간 | 처리 |
| --- | --- |
| `< RISK_MIN_CONFIDENCE` (기본 0.6) | 주문 후보 제외(HOLD 취급) |
| `0.6 ~ 0.75` | 후보 채택, **비중 축소 적용** |
| `> 0.75` | 정상 후보(여전히 한도 검증 필수) |

- 신뢰도는 **주문 강제 사유가 아니다.** 높은 신뢰도라도 위험 한도를 넘으면 거절된다.
- 신뢰도 산출 근거는 가능한 한 `rationale`에 요약한다.

---

## 6. 신호 만료(유효 시간) 규칙

- 모든 신호는 `validUntil`을 가진다. **이 시각을 넘기면 무효**이며 주문 후보가 될 수 없다.
- 권장 만료 기준(예시):
  - `INTRADAY`: 당일 장 마감 또는 수 분~수십 분
  - `SWING`: 수 시간 ~ 수일
  - `POSITION`: 수일 ~ 수주(재평가 주기 명시)
- 만료된 신호로 들어온 주문 후보는 Risk Engine이 **신호 만료**로 거절한다.
- 시장 국면이 신호 생성 시점과 크게 달라지면(레짐 전환) 미실행 신호는 재평가/폐기한다.

---

## 7. 저장 및 분리 원칙

- `trading_signal`: AI 신호(append-only, 수정 불가)
- `signal_input_snapshot`: 입력 요약(재현용)
- 주문 결과는 `order`/`order_execution`에 **별도 저장**한다(절대 규칙 7).
- 신호 ↔ 주문은 참조 키로 연결하되 한 테이블에 합치지 않는다.

---

## 8. 금지 사항

- 증권사 주문/취소 API 직접 호출 ❌
- 위험 한도 우회 또는 한도 변경 요청 ❌
- 비밀정보(키/토큰/계좌번호) 입력·출력·로그 포함 ❌
- 승인되지 않은 전략의 즉흥 실행 ❌
- 특정 종목 매수를 보장/추천하는 외부 공유 문서 작성 ❌

---

## 9. 관련 문서

- [RISK_POLICY.md](RISK_POLICY.md) · [STRATEGY_GOVERNANCE.md](STRATEGY_GOVERNANCE.md)
- [ADR-003 AI 트레이더 거버넌스](docs/decision-log/ADR-003-ai-trader-governance.md)
