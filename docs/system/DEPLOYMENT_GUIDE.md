# DEPLOYMENT_GUIDE — 배포 가이드

> 환경 구성, 배포 절차, 모드 전환, 롤백을 정의한다.
> 비밀정보는 환경 변수/Secret Manager로만 주입한다(기존 보안 정책 준수).

관련: [OBSERVABILITY](OBSERVABILITY.md) · [FAILURE_AND_RECOVERY](FAILURE_AND_RECOVERY.md) · [RISK_ENGINE_RULES](RISK_ENGINE_RULES.md)

---

## 1. 환경 구분

| 환경 | 용도 | 기본 모드 |
| --- | --- | --- |
| local | 개발 | PAPER / BACKTEST |
| staging | 통합 검증 | PAPER |
| prod | 실거래 | SEMI_AUTO → AUTO(소액) → 확대 |

> **prod 최초 기동은 절대 AUTO로 시작하지 않는다.** SEMI_AUTO부터 시작한다.

---

## 2. 구성 요소

| 구성 | 비고 |
| --- | --- |
| 애플리케이션 서비스 | Spring Boot 멀티모듈 컨테이너 |
| MySQL | 영속 데이터 |
| Redis | 캐시/멱등/락/레이트리미트 |
| 관측 스택 | Prometheus/Grafana(선택) |

증권사 자격증명은 **order/execution 계층에만** 주입한다(권한 분리).

---

## 3. 배포 절차

```mermaid
flowchart LR
    A[빌드 + 테스트 통과] --> B[이미지 생성/스캔]
    B --> C[staging 배포 PAPER]
    C --> D[헬스/검증/관측 확인]
    D --> E{승격 승인?}
    E -->|아니오| C
    E -->|예| F[prod 배포 SEMI_AUTO]
    F --> G[관찰 기간]
    G --> H[AUTO 소액 승격]
    H --> I[한도 단계적 확대]
```

### 3.1 배포 전 체크리스트
- [ ] 단위/통합 테스트 통과(거절 케이스 포함)
- [ ] 비밀정보 미포함(스캔 통과)
- [ ] `TRADING_MODE` 의도값 확인
- [ ] 위험 한도값이 정책과 일치
- [ ] Kill Switch 동작 점검
- [ ] DB 마이그레이션 호환성 확인
- [ ] 롤백 계획 확인

---

## 4. 모드/한도 전환 운영

- 모드 전환(SEMI_AUTO↔AUTO)은 관리자 API + 승인, **감사 로그** 기록.
- 한도 상향은 별도 승인 후 적용. 하향은 즉시 가능(보수적).
- 신규 AUTO 전환/한도 상향 직후 관찰 기간 동안 일부 주문을 승인 전환할 수 있다.

---

## 5. 헬스체크 / 무중단

- 각 서비스 readiness/liveness 프로브.
- 배포 중에는 **신규 주문을 일시 보류**하고 인플라이트 주문 상태를 동기화 후 전환(주문 유실/중복 방지).
- DB 마이그레이션은 하위호환(expand/contract) 패턴 권장.

---

## 6. 롤백

- 애플리케이션: 이전 이미지로 롤백, 스키마는 하위호환 유지로 무중단.
- 이상 시 **즉시 Kill Switch(전역) → 롤백 → 재동기화 → 수동 해제** 순서.
- 롤백 후 미체결/부분체결 주문을 증권사 상태 기준으로 정리한다.

---

## 7. 백업/보존

- 신호·주문·감사 데이터 정기 백업.
- 감사 로그는 변경 불가 보존(보존 기간은 운영 정책).
- 백업/복구 절차를 정기 리허설한다.

---

## 8. 운영 안전 수칙

- prod에서 데이터 피드 불안정 시 자동 보류/Kill을 신뢰한다.
- 사고 시 [FAILURE_AND_RECOVERY](FAILURE_AND_RECOVERY.md) 복구 체크리스트를 따른다.
- 모든 위험 조작(모드/한도/KillSwitch)은 사람 승인 + 감사 기록.
