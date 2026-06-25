# ArborBot — 개인용 AI 자동매매 플랫폼

> AI가 전업 트레이더처럼 시장을 분석해 **매수·매도·관망 신호와 권장 비중, 손절·익절 기준**을 생성하고,
> **Risk Engine과 Order Policy 검증을 통과한 신호만** 토스증권 Open API를 통해 자동/반자동으로
> 주문 실행하는 개인용 자동매매 시스템입니다. (Maven 멀티모듈 / Spring Boot 3 / Java 21)

> ⚠️ **수익을 보장하지 않습니다.** 목표는 수익 극대화가 아니라
> **위험 조정 수익률, 손실 제한, 안정적인 주문 실행, 재현 가능한 전략 검증**입니다.
> 모든 투자 결과·손실의 책임은 사용자 본인에게 있습니다.

---

## 1. 왜 만드는가 (목적)

- **개인 계좌 운용 자동화**: 시장 데이터·기술 지표·전략 성과·포트폴리오 상태를 종합해 사람이 매번 판단하지 않아도
  일관된 규칙으로 투자 신호를 만들고, 안전장치를 통과한 경우에만 실제 주문을 낸다.
- **안전 우선 설계**: 자동매매의 가장 큰 위험은 "잘못된 주문이 자본을 빠르게 잠식"하는 것이다. 그래서
  **AI는 신호만 만들고 주문은 절대 직접 내지 않으며**, 모든 주문은 다층 위험 검증을 통과해야 한다.
- **재현 가능한 전략 운영**: 전략 성과를 정량 지표로 추적하고, 기준 미달 전략은 자동 비활성화한다.
- **학습·연구 목적**: 퀀트 전략 연구 + 금융 시스템 아키텍처(헥사고날, Outbox, Resilience, 감사 로그) 실습.

### 절대 규칙 (시스템 불변식)
1. AI는 증권사 주문 API를 직접 호출하지 않는다. **AI는 `TradingSignal`만 생성한다.**
2. 모든 주문은 **Risk Engine + Order Policy** 검증을 통과해야 실행된다.
3. 수익률이 높아도 **위험 한도를 초과하면 주문하지 않는다.**
4. AI 신호와 실제 주문 결과는 **분리 저장**한다.
5. API Key·Secret·계좌번호·토큰은 **코드·로그·문서·Git에 절대 포함하지 않는다.**
6. 토스증권 API 상세 스펙은 **공식 문서 확인 전까지 추측하지 않는다**(Mock 어댑터 우선).

---

## 2. 무엇을 하는가 (전체 흐름)

```
시장데이터 → 국면판단 → 활성전략선택 → AI 신호생성 → 신호품질검증 → 포트폴리오 영향분석
   → Risk Engine 검증 → (자동 / 승인대기 / 거절) → 주문생성 → 토스 API 제출 → 체결 동기화
   → 포트폴리오·손익 갱신 → 전략 성과 재평가 → (전략 비중조정/중단) → 알림·감사·모니터링
```

`TradingSignal` 출력: `signalType(BUY/SELL/HOLD)`, `confidenceScore(0~100)`,
`recommendedPositionSizePercent`, `entryPriceMin/Max`, `stopLossPrice`, `takeProfitPrice`,
`holdingPeriod`, `validUntil`, `strategyId`, `marketRegime`, `rationale`, `riskFlags`, `createdAt`.

자세한 흐름·다이어그램은 [docs/system/](docs/system/) 참고.

---

## 3. 현재 구현 상태

| 영역 | 상태 |
| --- | --- |
| 멀티모듈 골격(common/domain/application/infrastructure/api) | ✅ 동작 |
| AI 신호 생성(Mock 휴리스틱) + 신호 품질 게이트 | ✅ |
| Risk Engine(Kill Switch·한도·중복·빈도·잔고·신뢰도·거래정지/유동성) | ✅ |
| 주문 상태기계(11상태) + 자동/승인대기/거절 분기 | ✅ |
| 브로커 어댑터: Mock(기본) + **실 REST 어댑터(설정 주입형)** + Resilience4j + 멱등키 | ✅ (실 스펙 값만 채우면 연동) |
| Outbox 제출 + 디스패처 재시도 | ✅ |
| 타임아웃 결과 불명 복구(주문 조회 재동기화, 맹목 재전송 금지) | ✅ |
| 체결 → 포지션·현금 갱신, 매도 실현손실 → 일일 손실 한도 반영 | ✅ |
| batch-service(주문 동기화·전략 성과 재평가/비활성화·일일 정산) | ✅ |
| market-data(**Mock**, instrument 시드) | ✅ (실 피드 연동 전) |
| Flyway 마이그레이션 + JPA 영속화 + 감사 로그 | ✅ |
| 실 증권사 주문 연동 | 🟡 어댑터 완비(`RestBrokerAdapter`), 스펙 값 주입 시 동작. 토스 개인 주문 API 미공개 — [연동 가이드](docs/integration/TOSS_BROKER_INTEGRATION.md) |
| Redis 레이트리미트 / 분산 Outbox 락 | ⛔ 보류(인프라 필요) |

> 기본 실행은 **Mock 브로커 + H2 인메모리**로 Docker 없이 전 과정을 시연/테스트할 수 있습니다.

---

## 4. 아키텍처 (모듈)

```
ArborBot (parent pom)
├─ common               비밀정보 마스킹, ID 생성, 공통 예외
├─ trading-domain       도메인 객체 + 주문 상태기계 (순수 Java)
├─ trading-application  유스케이스 + 포트(out 인터페이스)
│   └ service/{aitrader, strategyengine, portfolio, risk, execution, marketdata, batch}
├─ trading-infrastructure  JPA/Flyway, MockTossBrokerAdapter, MockMarketDataAdapter, Outbox, 배치, 알림
└─ trading-api          부트 앱: REST API, Actuator, OpenAPI/Swagger
```

요청 사양의 14개 서비스(market-data/strategy-engine/ai-trader/portfolio/risk-engine/execution/
broker-toss-adapter/notification/batch 등)는 위 계층의 **패키지/어댑터로 매핑**되어 있으며,
추후 독립 서비스로 분리 가능합니다(자세한 매핑은 [CLAUDE.md](CLAUDE.md) 참고).

---

## 5. 빠른 시작

### 사전 요구사항
- JDK 21 / (Maven 미설치 시 동봉된 `./mvnw` 래퍼 사용) / Docker(선택, MySQL 프로파일용)

### 빌드 & 테스트
```bash
./mvnw clean install          # 전체 모듈 빌드 + 테스트 (H2, Docker 불필요)
./mvnw -pl trading-domain test # 주문 상태기계 단위 테스트
./mvnw -pl trading-api test    # 신호→주문→체결 / KillSwitch·거래정지·손실한도·타임아웃복구 통합 테스트
```

### 실행 (기본 local 프로파일 = H2 인메모리)
```bash
./mvnw -pl trading-api spring-boot:run
# 또는
java -jar trading-api/target/trading-api-0.1.0-SNAPSHOT.jar
```
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

### MySQL 프로파일로 실행
```bash
cp .env.example .env          # 비밀값은 로컬에서만 (이 파일은 Git 추적 제외)
docker compose up -d mysql redis
java -jar trading-api/target/trading-api-0.1.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

---

## 6. API 사용 예시 (Mock 주문 흐름)

```bash
# 1) AI가 BUY 신호 생성 (AI는 주문을 내지 않음)
curl -s -X POST localhost:8080/api/v1/signals -H 'Content-Type: application/json' \
  -d '{"symbol":"005930","strategyId":"momentum-v1","requestedType":"BUY"}'

# 2) 신호로 주문 생성 → 기본은 approvalRequired=true 이므로 PENDING_APPROVAL
curl -s -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"signalId":"<위 응답의 signalId>"}'

# 3) 승인 → Mock 토스 어댑터로 제출/체결 (FILLED)
curl -s -X POST localhost:8080/api/v1/orders/<orderId>/approve

# 안전장치: 전역 Kill Switch ON → 이후 신규 매수는 모두 REJECTED
curl -s -X POST localhost:8080/api/v1/risk/kill-switch/enable -d '{"reason":"manual"}'
curl -s localhost:8080/api/v1/risk/status
```

주요 엔드포인트: `/api/v1/signals`, `/api/v1/orders`(+`/{id}/cancel`,`/approve`),
`/api/v1/strategies`(+`/performance`), `/api/v1/portfolio`, `/api/v1/risk/status`,
`/api/v1/risk/kill-switch/{enable,disable}`, `/api/v1/ai-trader/decisions`,
`/api/v1/batch/{sync-orders,reevaluate-strategies,daily-settlement}`, `/actuator/health`.

---

## 7. 안전장치 & 거래 모드

- **거래 모드**: `BACKTEST → PAPER → SEMI_AUTO → AUTO(소액) → 확대` (건너뛸 수 없음)
- **기본값(안전 우선)**: `autoTradingEnabled=false`, `approvalRequired=true`, `dryRun=true`, `mode=PAPER`
- **자동 주문 조건(모두 충족 시)**: 모드 AUTO + 전략 자동적격 + Kill Switch OFF + Risk 통과 +
  신뢰도 임계 이상 + 신호 유효 + 주문금액 자동 상한 이내
- **Kill Switch**: 전역/전략/종목 단위 즉시 차단(자동 발동·수동 해제)
- **위험 한도**: 1회/일일 주문금액, 일일 손실 한도, 종목·섹터·현금 비중, 중복·빈도 — `.env`(`RISK_*`)/설정으로 관리

상세는 [RISK_POLICY.md](RISK_POLICY.md), [docs/system/RISK_ENGINE_RULES.md](docs/system/RISK_ENGINE_RULES.md) 참고.

---

## 8. 문서

| 문서 | 내용 |
| --- | --- |
| [CLAUDE.md](CLAUDE.md) | **다른 환경에서 Claude Code로 개발을 이어가기 위한 컨텍스트**(구조·규칙·명령·관례) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 아키텍처·컴포넌트·데이터 흐름 |
| [AI_TRADER_POLICY.md](AI_TRADER_POLICY.md) | AI 입력/출력·전략 선택·신뢰도·만료 |
| [RISK_POLICY.md](RISK_POLICY.md) | 주문 차단 조건·한도·Kill Switch |
| [STRATEGY_GOVERNANCE.md](STRATEGY_GOVERNANCE.md) | 전략 등록·검증·승격·중단 |
| [API_CONTRACT.md](API_CONTRACT.md) | 내부 API 계약·DTO |
| [SECURITY.md](SECURITY.md) | 비밀정보·로그 마스킹·감사·권한 |
| [docs/integration/TOSS_BROKER_INTEGRATION.md](docs/integration/TOSS_BROKER_INTEGRATION.md) | 실 증권사 REST 연동·설정·보안 체크리스트 |
| [docs/system/](docs/system/) | 흐름·다이어그램(Mermaid)·운영 |
| [docs/decision-log/](docs/decision-log/) | 아키텍처 결정 기록(ADR) |

---

## 9. 로드맵 (다음 작업)

- [x] 부분 체결(`PARTIALLY_FILLED`) 처리 + 잔량 정책
- [x] T+2 결제(매수 직후 매도가능수량 미증가 → 결제 후 전환)
- [x] 실 증권사 REST 어댑터(설정 주입형) — 스펙 값 입력 시 동작 ([연동 가이드](docs/integration/TOSS_BROKER_INTEGRATION.md))
- [ ] market-data 실수집 어댑터(시세/호가/거래정지 피드)
- [ ] 실 브로커 스펙 확정 후 값 입력 + 모의투자 검증(토스 개인 주문 API 미공개 → KIS/키움 등 공식 API 대상)
- [ ] Redis 기반 레이트리미트/중복 판정, 분산 Outbox 락
- [ ] Python 전략 연구·백테스트 모듈(`research/`)

---

## 10. 면책

본 저장소는 개인 학습·연구 목적입니다. 수익을 보장하지 않으며, 시스템 사용으로 발생하는 모든 투자
결과·손실의 책임은 사용자 본인에게 있습니다. 실제 운용 전 충분한 백테스트·페이퍼 트레이딩·소액 검증을 거치십시오.
