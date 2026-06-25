# DEVELOPMENT_GUIDE — 개발 가이드 (외부 공유용)

> 로컬에서 시스템을 띄우고 개발/테스트하는 방법입니다.
> **실제 키/토큰/계좌번호는 로컬 `.env` 에만 두고 절대 커밋하지 마십시오.**

---

## 1. 사전 준비

| 도구 | 버전 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Docker / Compose | 최신 |
| Python | 3.11+ (research) |

## 2. 환경 변수

```bash
cp .env.example .env
# .env 를 열어 로컬 값 입력 (비밀값은 로컬 전용, .gitignore 로 제외됨)
```

- 최초 기동은 **`TRADING_MODE=PAPER`** 또는 `BACKTEST` 로 시작하십시오.
- 위험 한도(`RISK_*`)는 [RISK_POLICY.md](../../RISK_POLICY.md)와 일치시키십시오.

## 3. 인프라 기동

```bash
docker compose up -d mysql redis
docker compose ps
```

## 4. 백엔드 빌드/실행

```bash
cd backend
./mvnw clean install            # 전체 모듈 빌드
./mvnw -pl api-gateway spring-boot:run
```

- Swagger: `http://localhost:8080/swagger-ui.html` (서비스별 포트/경로 상이)

## 5. 테스트

```bash
cd backend
./mvnw test       # 단위
./mvnw verify     # 통합(Testcontainers, Docker 필요)
./mvnw -pl risk-engine test
```

- **위험/주문 로직 변경 시 거절 케이스 테스트는 필수**입니다([CONTRIBUTING.md](../../CONTRIBUTING.md)).

## 6. Docker 전체 기동

```bash
docker compose up -d --build
docker compose logs -f
docker compose down
```

## 7. Python research 모듈

```bash
cd research
python -m venv .venv && source .venv/bin/activate
pip install -e .
pytest
```

## 8. 개발 시 안전 수칙

- ai-trader-service 에 broker 의존성을 추가하지 마십시오(주문 불가 원칙).
- 로그/테스트/픽스처에 실제 계좌·토큰·키를 넣지 마십시오.
- 증권사 API 응답 필드는 공식 문서 확정 전 추측해 하드코딩하지 마십시오.
- 한도/Kill Switch 관련 변경은 정책 문서와 동기화하십시오.

## 9. 자주 겪는 문제

| 증상 | 확인 |
| --- | --- |
| 통합 테스트 실패 | Docker 데몬 실행 여부(Testcontainers) |
| DB 접속 실패 | `.env` MYSQL_* 값, 컨테이너 health |
| 기동 모드 의도와 다름 | `TRADING_MODE` 환경 변수 |
| 비밀값 노출 경고 | `.env` 가 커밋되지 않았는지(gitleaks) |

## 10. 관련 문서

- [README.md](../../README.md) · [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [CONTRIBUTING.md](../../CONTRIBUTING.md) · [SECURITY.md](../../SECURITY.md)
