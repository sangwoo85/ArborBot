# CLAUDE.md — ArborBot 개발 컨텍스트

이 파일은 **Claude Code가 이 저장소에서 개발을 이어갈 때 먼저 읽는 컨텍스트**입니다.
ArborBot은 개인용 AI 자동매매 플랫폼입니다. 제품 개요/사용법은 [README.md](README.md),
정책·흐름은 [docs/](docs/)와 루트 정책 문서들을 참조하세요.

---

## 0. 절대 규칙 (코드 변경 시 반드시 준수)

1. **AI는 증권사 주문 API를 직접 호출하지 않는다.** `ai-trader-service`(= `service.aitrader`)는
   `BrokerPort`/주문 서비스에 **의존조차 하지 않는다**(컴파일 단계에서 차단). 깨뜨리지 말 것.
2. 모든 주문은 `RiskEngine` 검증을 통과해야 실행된다. 검증 우회 코드 금지.
3. 수익률이 높아도 위험 한도 초과 주문은 실행 금지.
4. 신호(`trading_signal`)와 주문(`orders`/`order_execution`)은 분리 저장.
5. **비밀정보(키/시크릿/계좌번호/토큰)를 코드·로그·테스트·문서·Git에 넣지 말 것.** 로그는 `SensitiveDataMasker`로 마스킹.
6. **토스증권 API 실제 스펙을 추측해 하드코딩하지 말 것.** 연동 지점은 `MockTossBrokerAdapter` 하나뿐.
7. 기본값은 안전한 쪽: `autoTradingEnabled=false`, `approvalRequired=true`, `dryRun=true`.

---

## 1. 기술 스택 / 빌드

- Java 21, Spring Boot 3.3.5, Maven 멀티모듈. MySQL 8(운영) / H2 인메모리(로컬·테스트). Flyway, JPA/Hibernate.
- Resilience4j(Retry/CircuitBreaker), springdoc-openapi, Spring Actuator. **Lombok·MapStruct 미사용**(순수 Java).
- 빌드/실행/테스트 (Maven 미설치 시 `./mvnw` 래퍼 사용):
  ```bash
  ./mvnw clean install                     # 전체 빌드 + 테스트
  ./mvnw -pl trading-api spring-boot:run    # 앱 실행 (H2, :8080)
  java -jar trading-api/target/trading-api-0.1.0-SNAPSHOT.jar [--spring.profiles.active=mysql]
  ./mvnw -pl <module> test                  # 모듈 단위 테스트
  ```
  > Homebrew로 maven 설치 시 PATH: `export PATH="/opt/homebrew/bin:$PATH"`.

---

## 2. 모듈 구조 & 책임

```
common               마스킹/ID/예외 (의존 없음)
trading-domain       도메인 객체 + 주문 상태기계 (순수 Java, Spring 없음)
trading-application  유스케이스 + 포트(out 인터페이스) + 설정(TradingProperties)
trading-infrastructure  포트 구현(JPA 어댑터, Mock 브로커/마켓데이터, Outbox, 배치, 알림) + Flyway
trading-api          부트 진입점 + REST 컨트롤러 + 예외처리 + OpenAPI
```

요청 사양 14개 서비스 → 본 구조 매핑:
| 사양 모듈 | 위치 |
| --- | --- |
| market-data-service | `application/service/marketdata` + `infrastructure/marketdata/MockMarketDataAdapter` |
| strategy-engine | `application/service/strategyengine` |
| ai-trader-service | `application/service/aitrader` (주문 의존 없음) |
| portfolio-service | `application/service/portfolio` |
| risk-engine | `application/service/risk` |
| execution-service | `application/service/execution` (`OrderService`, `ExecutionSubmitter`) |
| broker-toss-adapter | `infrastructure/broker/MockTossBrokerAdapter` (`BrokerPort` 구현) |
| notification-service | `infrastructure/notification` |
| batch-service | `application/service/batch` + `infrastructure/batch/ScheduledBatchJobs` |

핵심 흐름은 `OrderService.createOrderFromSignal()`: 신호 품질검증 → 포트폴리오 영향분석 →
주문생성(CREATED) → VALIDATING → `RiskEngine.evaluate` → (REJECT/PENDING_APPROVAL/APPROVED) →
APPROVED면 Outbox enqueue + `ExecutionSubmitter.submit`(Mock 브로커, 체결 시 포지션/현금 갱신).

---

## 3. 코드 관례 (중요 — 안 지키면 깨짐)

- **값 객체/DTO는 Java `record`**, **JPA 엔티티는 일반 클래스 + getter/setter**(레코드 불가).
- **Spring 물리 네이밍 전략**: 엔티티 camelCase 필드 → DB **snake_case** 컬럼. Flyway DDL은 snake_case로 작성해야 한다.
  (예: `confidenceScore` → `confidence_score`)
- **예약어 회피**: 테이블 `orders`(order 금지), `positions`. 컬럼 `order_mode`(`mode` 예약어 회피, `OrderEntity`에서 `@Column(name="order_mode")`).
- **Resilience4j `@TimeLimiter` 금지(동기 메서드)**: `CompletableFuture` 반환에만 동작. Mock 브로커는 `@Retry`+`@CircuitBreaker`만.
  실제 timeout은 추후 실제 HTTP 클라이언트 요청 타임아웃으로 적용.
- 주문 상태 변경은 `Order.transitionTo()`로만(허용 전이만, `OrderStateMachine`). 영속 복원은 `Order.restoreState()`.
- 헥사고날: application은 `port.out.*` 인터페이스에만 의존, 구현은 infrastructure 어댑터(`@Repository`/`@Component`).
- 시간은 `Instant`(UTC 저장), 금액 `BigDecimal`, 비율 percent 단위.

---

## 4. DB / 마이그레이션

- Flyway: `trading-infrastructure/src/main/resources/db/migration/` (`V1__init`, `V2__seed`, `V3__instrument`).
  스키마 변경은 **새 버전 파일 추가**(기존 적용본 수정 금지). DDL은 H2(MySQL 모드)+MySQL 양쪽 호환되게.
- `ddl-auto=none`(Flyway가 스키마 관리). 시드: 전략 3개(momentum-v1 건전, mean-reversion-v1·experimental-v0 부적격),
  계좌잔고, 포지션(000660, 005930), instrument(정상/거래정지 900110/유동성부족 900120) — **모두 가상 데이터**.

---

## 5. 테스트 방식

- 위치: 도메인 단위 테스트는 `trading-domain/src/test`, 통합 테스트는 `trading-api/src/test`(`@SpringBootTest`).
- **테스트 격리**: 각 통합 테스트 클래스는 고유 H2 인메모리 DB를 `properties`로 지정.
  예: `spring.datasource.url=jdbc:h2:mem:trading_xxx;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`.
  (같은 이름 mem DB는 JVM 내 공유되므로 테스트마다 이름을 다르게.)
- 위험/주문 로직 변경 시 **거절 케이스 테스트 필수**. 현재 통과 테스트(17개)는 회귀 가드:
  상태기계, 기본흐름(PENDING→approve→FILLED), 자동주문, KillSwitch 거절, 거래정지/유동성 거절,
  일일손실 한도 차단, 타임아웃 재동기화, 부분 체결 수렴, 포지션 갱신, 전략 재평가, 스모크.
- 기대 동작이 "거절"인데 신뢰도/한도 때문에 의도와 다르게 막히면, 테스트 `properties`로
  `trading.limits.*`(min-confidence-score, max-order-amount 등)를 조정해 의도한 한 가지만 검증.

---

## 6. 현재 상태 & 다음 작업

**구현됨**: 신호생성(Mock)·품질게이트, RiskEngine(KillSwitch/한도/중복/빈도/잔고/신뢰도/거래정지·유동성),
주문 상태기계·자동/승인/거절, Mock 토스 어댑터(Retry/CB/멱등/조회), Outbox+디스패처,
타임아웃 결과불명 복구(맹목 재전송 금지), **부분 체결 처리(delta 누적·잔량 폴링·수렴)**,
체결→포지션/현금 갱신, 매도 실현손실→일일손실 한도, batch(주문동기화·전략 재평가/비활성화·일일정산),
market-data(Mock), Flyway/JPA/감사로그/Actuator/Swagger.

> 부분 체결: 브로커 응답의 **누적** 체결수량과 이미 반영분의 차이(delta)만 처리(`ExecutionSubmitter.applyAccepted`).
> 부분 체결 동안 Outbox 는 PENDING 유지 → 디스패처/배치(`syncSubmittedOrders`)가 잔량을 폴링해 FILLED 로 수렴.
> 상태기계는 `PARTIALLY_FILLED → PARTIALLY_FILLED` 자기 전이 허용.

**다음 작업(우선순위 순)**:
1. T+2 결제 — 매수 직후 `sellableQuantity` 증가하지 않도록 보정(현재 즉시 증가).
2. market-data 실수집 어댑터(시세/호가/거래정지 피드)로 `MockMarketDataAdapter` 대체.
3. 토스증권 실제 API 연동(인증/주문/취소/잔고/체결 조회)으로 `MockTossBrokerAdapter` 대체 — **공식 문서 확인 후**.
4. Redis 레이트리미트/중복 판정, 분산 Outbox 락(다중 인스턴스).
5. `research/`(Python) 백테스트 모듈.

**보류(이유)**: Redis/분산 락은 추가 인프라 필요 → 단일 인스턴스 가정 하에 DB 기반으로 동작 중.

---

## 7. 작업 시작 체크리스트 (Claude Code)

- [ ] `./mvnw -q -DskipTests install`로 빌드 확인 후 작업 시작.
- [ ] 변경 후 `./mvnw test`로 회귀 확인(16개 유지). 위험/주문 변경 시 거절 케이스 테스트 추가.
- [ ] 새 DB 컬럼/테이블은 새 Flyway 버전 + snake_case + 양쪽 DB 호환.
- [ ] 절대 규칙(0장) 위반 여부 self-check, 특히 ai-trader가 broker에 의존하지 않는지.
- [ ] 비밀정보 노출/로그 마스킹 확인. 커밋은 사용자가 요청할 때만.
