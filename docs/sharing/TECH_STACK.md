# TECH_STACK — 기술 스택 (외부 공유용)

> 본 문서는 비밀정보를 포함하지 않습니다.

## 백엔드

| 항목 | 선택 | 비고 |
| --- | --- | --- |
| 언어 | Java 21 | LTS, 가상 스레드 등 활용 가능 |
| 프레임워크 | Spring Boot 3.x | 멀티모듈 |
| 빌드 | Maven | 부모 POM 집계 |
| API 문서 | OpenAPI / Swagger | 런타임 명세 노출 |

## 데이터 / 인프라

| 항목 | 선택 | 용도 |
| --- | --- | --- |
| RDB | MySQL 8.x | 신호/주문/포지션/감사 영속화 |
| 캐시 | Redis | 시세 캐시, 중복·빈도 제한, 멱등/락 |
| 컨테이너 | Docker / Docker Compose | 로컬·단일 호스트 기동 |
| 관측(선택) | Prometheus / Grafana | 메트릭/대시보드 |

## 테스트

| 항목 | 선택 |
| --- | --- |
| 단위 | JUnit 5, Mockito |
| 통합 | Testcontainers (MySQL/Redis) |
| 보안 | gitleaks(비밀 스캔), 의존성 취약점 스캔 |

## 전략 연구

| 항목 | 선택 | 비고 |
| --- | --- | --- |
| 언어 | Python 3.11+ | research/ 모듈 |
| 용도 | 백테스트·전략 연구·검증 | 재현 가능성 우선 |

## 외부 연동

| 항목 | 선택 | 비고 |
| --- | --- | --- |
| 증권사 | 토스증권 Open API | **스펙은 공식 문서 확정 전 추측 금지** |
| AI 분석(선택) | LLM/모델 제공자 | 키는 환경 변수로만 주입 |

## 모듈 의존 원칙

- `ai-trader-service` 는 broker SDK / `order-service` 에 의존하지 않는다(주문 불가 강제).
- 증권사 자격증명은 `order-service` 에만 노출한다(권한 분리).
- 모듈 간 계약은 [API_CONTRACT.md](../../API_CONTRACT.md) 기준.

## 관련 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md) · [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
