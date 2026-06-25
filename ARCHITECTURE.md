# ARCHITECTURE.md — 시스템 아키텍처

본 문서는 AI 자동매매 플랫폼의 컴포넌트, 책임 분리, 데이터 흐름, 저장소 구조를 설명합니다.
핵심 불변식은 **"AI는 신호만 생성하고, 주문은 Risk Engine을 통과한 경우에만 단일 실행 지점에서 발생한다"** 입니다.

---

## 1. 설계 목표

| 목표 | 설명 |
| --- | --- |
| 안전한 주문 실행 | 모든 주문은 다층 검증을 통과해야 한다 |
| 책임 분리 | 신호 생성 / 위험 검증 / 주문 실행을 물리적으로 분리한다 |
| 재현성 | 동일 입력 → 동일 백테스트 결과, 신호·입력 요약 저장 |
| 관측 가능성 | 신호, 주문, 거절 사유, 한도 상태를 추적 가능 |
| 위험 우선 | 개별 종목 수익보다 포트폴리오 전체 위험을 우선한다 |

---

## 2. 컴포넌트 (Spring Boot 멀티모듈)

```
backend/
├── common              공통 도메인/DTO/예외/마스킹 유틸/시간(Clock) 추상화
├── api-gateway         외부 진입점, 인증, 관리자 API, 요청 라우팅
├── market-data-service 시세·기술지표·뉴스/이벤트 신호 수집·정규화·캐싱(Redis)
├── ai-trader-service   시장국면 분석 + TradingSignal 생성   ★주문 호출 불가★
├── strategy-service    전략 등록/성과추적/활성화, 백테스트(Python) 연동
├── risk-engine         주문 사전검증, 한도/중복/빈도/Kill Switch 판정
├── order-service       OrderPolicy + Execution             ★유일한 증권사 API 호출 지점★
├── portfolio-service   포지션·잔고·섹터 비중·평가손익 집계
└── audit-service       신호/주문/감사로그 분리 저장
```

### 2.1 책임 경계 (불변식)

| 컴포넌트 | 할 수 있는 것 | 절대 못 하는 것 |
| --- | --- | --- |
| ai-trader-service | TradingSignal 생성, 근거·입력요약 저장 | 증권사 주문 API 호출, 잔고 변경 |
| risk-engine | 주문 후보 검증/거절, 한도 조회 | 신호 생성, 주문 실행 |
| order-service | 검증 통과 주문만 실행, 결과 저장 | 위험 검증 우회, 신호 생성 |
| audit-service | 신호/주문/거절 기록 | 신호 수정, 주문 실행 |

> ai-trader-service 모듈은 빌드 의존성에서 `order-service`/증권사 SDK를 **포함하지 않습니다.**
> 컴파일 타임에 호출 자체가 불가능하도록 구성합니다(ADR-002 참고).

---

## 3. 데이터 흐름 (신호 → 주문)

```
(1) 수집
    market-data-service → 시세/지표/뉴스 정규화 → Redis 캐시 + MySQL 적재

(2) 분석/신호 생성
    ai-trader-service
      ├─ 시장 국면 분석 (상승/하락/횡보/고변동성/이벤트)
      ├─ strategy-service 에서 활성 전략 후보 조회 (최근 성과 기반)
      ├─ portfolio-service 에서 현재 포지션/비중 조회
      └─ TradingSignal 생성 (BUY/SELL/HOLD + 신뢰도/비중/진입·손절·익절/유효기간/근거)
            │
            ▼  (신호 + 입력요약 저장: audit-service)

(3) 위험 검증
    risk-engine
      └─ OrderCandidate(신호 → 주문 후보) 다층 검증
            ├─ 통과 → order-service 로 전달
            └─ 거절 → 사유 기록(audit), 종료

(4) 주문 정책 + 실행
    order-service
      ├─ OrderPolicy 재검증 (모드: PAPER/SEMI_AUTO/AUTO)
      ├─ SEMI_AUTO 면 사람 승인 대기
      └─ Execution → 토스증권 Open API 호출 → 체결/실패 결과 저장(audit)

(5) 반영
    portfolio-service 가 체결을 반영해 포지션/비중/손익 갱신
    strategy-service 가 성과를 누적해 전략 활성/비중 조정
```

신호와 주문 결과는 **서로 다른 테이블/도메인**으로 분리 저장합니다(절대 규칙 7).

---

## 4. 거래 모드

| 모드 | 동작 | 증권사 주문 |
| --- | --- | --- |
| `BACKTEST` | 과거 데이터로 전략 검증 | 호출 없음 |
| `PAPER` | 실시간 신호 + 가상 체결 | 호출 없음 |
| `SEMI_AUTO` | 신호→검증→**사람 승인 후** 실행 | 승인 시 호출 |
| `AUTO` | 신호→검증→자동 실행(소액 한도) | 자동 호출 |

모드 전환은 [STRATEGY_GOVERNANCE.md](STRATEGY_GOVERNANCE.md)의 승격 절차를 따릅니다.

---

## 5. 저장소 모델 (개념)

- `trading_signal` — AI 신호 (입력요약·근거 포함, **수정 불가/append-only**)
- `signal_input_snapshot` — 신호 생성 시 입력 데이터 요약(재현용)
- `order_candidate` — 신호로부터 파생된 주문 후보
- `risk_evaluation` — 검증 결과(통과/거절 + 위반 항목 목록)
- `order` / `order_execution` — 실제 주문 및 체결 결과
- `position` / `portfolio_snapshot` — 포지션/잔고/섹터 비중
- `strategy` / `strategy_performance` — 전략 메타 및 성과 시계열
- `audit_log` — 감사 로그(권한/모드변경/KillSwitch 등)
- `kill_switch_state` — Kill Switch 상태/이력

> 비밀정보(키/토큰/계좌번호)는 위 어떤 테이블에도 저장하지 않습니다. [SECURITY.md](SECURITY.md) 참고.

---

## 6. 인프라

- **MySQL 8.x** — 영속 데이터(신호/주문/포지션/감사)
- **Redis** — 시세 캐시, 주문 빈도/중복 판정(레이트리미트), 분산 락
- **Docker Compose** — 로컬/단일 호스트 기동
- **Prometheus/Grafana**(deploy/) — 메트릭/대시보드(선택)

---

## 7. 관측 가능성 핵심 지표

- 신호 생성 수 / 거절 수 / 거절 사유 분포
- 주문 성공·실패율, 체결 지연
- 일일 누적 주문금액·손익 (한도 대비)
- Kill Switch 발동 횟수
- 전략별 성과(샤프, MDD, 승률)

---

## 8. 관련 결정 기록(ADR)

- [ADR-001 시스템 아키텍처](docs/decision-log/ADR-001-system-architecture.md)
- [ADR-002 주문 안전성](docs/decision-log/ADR-002-order-safety.md)
- [ADR-003 AI 트레이더 거버넌스](docs/decision-log/ADR-003-ai-trader-governance.md)
