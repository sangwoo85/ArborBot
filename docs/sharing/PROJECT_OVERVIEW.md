# PROJECT_OVERVIEW — 프로젝트 개요 (외부 공유용)

> 외부 개발자/협업자에게 프로젝트를 소개하기 위한 문서입니다.
> **실제 계좌·토큰·API Key·개인 투자 내역·추천 종목은 포함하지 않습니다.**

---

## 한 줄 소개

개인 계좌를 대상으로, AI가 시장을 분석해 투자 신호를 생성하고,
다층 위험 검증을 통과한 주문만 증권사 Open API로 실행하는 자동매매 플랫폼입니다.

## 목표

- **수익 보장 아님.** 목표는 **위험 조정 수익률, 손실 제한, 안정적 주문 실행, 재현 가능한 전략 검증**.
- 개별 종목 수익보다 **포트폴리오 전체 위험**을 우선한다.

## AI 트레이더가 하는 일

1. 시장 국면 분석(상승/하락/횡보/고변동성/이벤트)
2. 전략 성과 비교 → 활성 전략 선택
3. 종목별 신호 생성: `BUY/SELL/HOLD` + 신뢰도, 권장 비중, 진입/손절/익절,
   보유 기간, 유효 시간, 판단 근거
4. 성과 악화 전략 비활성화/비중 축소
5. 포트폴리오 위험 우선 고려

## 안전 설계 (핵심)

- AI는 **신호만** 만든다. 주문 API를 직접 호출하지 않는다.
- 주문은 **Risk Engine + Order Policy** 통과 후 **order-service**에서만 실행된다.
- 수익률이 높아도 **위험 한도를 넘으면 주문하지 않는다**.
- **Kill Switch**로 전역/전략/종목 단위 즉시 차단.
- 신호와 주문 결과는 **분리 저장**, 판단 근거·입력 요약 보관.

## 단계적 활성화

```
백테스트 → 페이퍼 트레이딩 → 반자동 승인 → 소액 자동주문 → 확대
```
각 단계는 정량 기준과 명시적 승인을 거칩니다. 건너뛸 수 없습니다.

## 구성 요소(요약)

| 구성 | 역할 |
| --- | --- |
| market-data-service | 시세·지표·뉴스 수집/정규화 |
| ai-trader-service | 시장 분석 + 신호 생성(주문 불가) |
| strategy-service | 전략 등록/성과/활성화 |
| risk-engine | 주문 사전 검증, 한도, Kill Switch |
| order-service | 검증 통과 주문만 실행(유일한 증권사 호출 지점) |
| portfolio-service | 포지션/잔고/비중 집계 |
| audit-service | 신호/주문/감사 분리 저장 |

## 더 보기

- 아키텍처: [ARCHITECTURE.md](../../ARCHITECTURE.md)
- 위험 정책: [RISK_POLICY.md](../../RISK_POLICY.md)
- AI 정책: [AI_TRADER_POLICY.md](../../AI_TRADER_POLICY.md)
- 전략 거버넌스: [STRATEGY_GOVERNANCE.md](../../STRATEGY_GOVERNANCE.md)
- 기술 스택: [TECH_STACK.md](TECH_STACK.md)
- 개발 가이드: [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)

## 면책

본 시스템은 수익을 보장하지 않으며, 모든 투자 결과·손실의 책임은 사용자 본인에게 있습니다.
