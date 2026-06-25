# research — ArborBot 전략 연구·백테스트 모듈

전략을 **재현 가능하게** 검증하기 위한 Python 백테스트 모듈입니다.
외부 데이터/무거운 의존성 없이 **표준 라이브러리만으로 오프라인 동작**합니다.
(실데이터 분석·시각화가 필요하면 `pip install -e ".[research]"` 로 numpy/pandas 선택 설치)

ArborBot 의 전체 안전 흐름(README 참고)에서 본 모듈은 **백테스트 단계**를 담당합니다:
`백테스트 → 페이퍼 트레이딩 → 반자동 승인 → 소액 자동주문 → 확대`.

---

## 구성

```
research/
├── arborbot_research/
│   ├── data.py       # 재현 가능한 합성 OHLCV 생성(시드 GBM) + CSV 로더
│   ├── strategy.py   # Strategy 베이스 + MA 교차 / 평균회귀 (롱 온리)
│   ├── engine.py     # 백테스터: 룩어헤드 금지(다음 봉 시가 체결), 수수료·슬리피지·손절/익절
│   ├── metrics.py    # 성과 지표(누적수익률·MDD·승률·손익비·샤프·거래횟수·거래비용 반영)
│   └── cli.py        # 커맨드라인 실행
└── tests/            # unittest (설치 불필요) — pytest 와도 호환
```

성과 지표는 [STRATEGY_GOVERNANCE.md](../STRATEGY_GOVERNANCE.md) 의 정의를 따르며,
**모든 수익률은 거래비용(수수료+슬리피지) 반영 기준**입니다.

---

## 실행

별도 설치 없이 표준 라이브러리만으로 동작합니다.

```bash
cd research

# 백테스트 실행 (합성 데이터)
python3 -m arborbot_research.cli --strategy ma --bars 500 --seed 42
python3 -m arborbot_research.cli --strategy meanrev --bars 600 --seed 7 --commission-bps 5 --slippage-bps 5

# 실데이터 사용 (CSV: date,open,high,low,close,volume)
python3 -m arborbot_research.cli --strategy ma --csv path/to/ohlcv.csv

# 테스트 (설치 불필요)
python3 -m unittest discover -s tests -v
# 또는 pytest 사용 시
pip install -e ".[dev]" && pytest
```

출력 예(JSON): `cumulative_return_pct, max_drawdown_pct, win_rate_pct, profit_factor,
sharpe_ratio, trade_count, net_return_after_cost_pct, total_cost`.

---

## 설계 원칙

- **재현성**: 동일 seed → 동일 데이터·동일 결과(테스트로 보장).
- **룩어헤드 금지**: 봉 i 종가로 판단, 봉 i+1 시가에 체결.
- **거래비용 반영**: 진입/청산마다 수수료·슬리피지 차감(미반영 성과는 신뢰하지 않음).
- **롱 온리**: 개인 계좌·공매도 없음 가정.
- **경계 안전**: 표본 부족 시 샤프/손익비 0 처리, 0 분모 가드.

> 본 모듈은 연구용이며 **수익을 보장하지 않습니다.** 백테스트 성과가 좋아도 과최적화 위험이 있으므로
> 페이퍼 트레이딩·소액 검증을 반드시 거치십시오([STRATEGY_GOVERNANCE.md](../STRATEGY_GOVERNANCE.md)).
