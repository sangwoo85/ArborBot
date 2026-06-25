# CONTRIBUTING.md — 기여 가이드

본 프로젝트에 기여하기 전에 [README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md),
[RISK_POLICY.md](RISK_POLICY.md), [SECURITY.md](SECURITY.md)를 먼저 읽어 주십시오.
이 시스템은 **실 자금 주문**을 다루므로, 안전 규칙이 코드 스타일보다 우선합니다.

---

## 1. 반드시 지켜야 할 불변 규칙

기여 코드는 다음을 위반할 수 없습니다(위반 PR은 반려).

1. AI(ai-trader-service)는 증권사 주문 API를 호출하지 않는다.
2. 주문 실행은 Risk Engine 통과 후 order-service에서만 일어난다.
3. 위험 한도를 우회하는 코드 금지.
4. 비밀정보(키/시크릿/계좌번호/토큰)를 코드·로그·테스트·문서·Git에 넣지 않는다.
5. 토스증권 API 스펙을 추측해 하드코딩하지 않는다(공식 문서 확정 전).
6. 신호와 주문 결과는 분리 저장한다.

---

## 2. 개발 환경

[docs/sharing/DEVELOPMENT_GUIDE.md](docs/sharing/DEVELOPMENT_GUIDE.md)를 참고하십시오.

```bash
cp .env.example .env          # 비밀값은 로컬에서만
docker compose up -d mysql redis
cd backend && ./mvnw clean install
```

---

## 3. 브랜치 전략

- `main` — 항상 빌드/테스트 통과. 직접 푸시 금지(PR 필수).
- `feature/<scope>-<요약>` — 기능 개발 (예: `feature/risk-daily-loss-limit`)
- `fix/<scope>-<요약>` — 버그 수정
- `docs/<요약>` — 문서

---

## 4. 커밋 메시지 (Conventional Commits)

```
<type>(<scope>): <요약>

본문(선택): 변경 이유/맥락
```

`type`: `feat | fix | docs | refactor | test | chore | perf | build | ci`
`scope`(예): `risk | order | ai-trader | strategy | market-data | portfolio | audit | common`

예시:
```
feat(risk): 일일 손실 한도 도달 시 신규 매수 차단
test(order): SEMI_AUTO 미승인 주문 미실행 검증
```

---

## 5. 코딩 규칙

- Java 21 / Spring Boot 3.x 컨벤션을 따른다.
- 도메인 경계를 침범하지 않는다(ai-trader가 broker 의존성 추가 ❌).
- 외부 입력(증권사 응답/뉴스)은 검증 후 사용한다.
- 시간은 `Clock` 추상화를 통해 사용한다(테스트 가능성).
- 금액은 정수(KRW), 비교는 경계조건을 명시한다.
- public API 변경 시 [API_CONTRACT.md](API_CONTRACT.md)와 Swagger를 함께 갱신한다.

---

## 6. 테스트 요구사항

- 위험/주문 로직 변경 시 **거절 케이스 테스트 필수**(통과/경계/초과).
- 단위 테스트(JUnit5/Mockito) + 통합 테스트(Testcontainers).
- 다음은 머지 차단(blocking) 대상:
  - Risk Engine 거절 규칙 누락/약화
  - Kill Switch 우회 가능 코드
  - 비밀정보 노출 가능 코드

```bash
cd backend && ./mvnw verify
```

---

## 7. Pull Request 체크리스트

- [ ] 불변 규칙(1장) 위반 없음
- [ ] 비밀정보 미포함(gitleaks 통과)
- [ ] 관련 문서 갱신(API/정책)
- [ ] 테스트 추가/통과, 거절 케이스 포함
- [ ] 위험 한도/Kill Switch에 영향 시 RISK_POLICY와 일치
- [ ] 거래 모드 기본값이 안전한가(PAPER 우선)

PR 설명에는 **위험에 미치는 영향**을 명시하십시오.

---

## 8. 리뷰 우선순위

risk-engine / order-service / 보안 관련 변경은 **최우선 리뷰** 대상이며,
최소 1인 이상의 명시적 승인이 필요합니다(가능하면 도메인 책임자).

---

## 9. 행동 규범

- 실제 투자 종목 추천/수익 보장 문구를 저장소에 남기지 않는다.
- 개인 계좌·거래 내역을 커밋하지 않는다.
- 보안 취약점은 [SECURITY.md](SECURITY.md)의 비공개 절차로 보고한다.
