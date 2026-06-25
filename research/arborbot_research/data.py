"""시세 데이터: 재현 가능한 합성 OHLCV 생성 + CSV 로더.

외부 시세 API 의존 없이 오프라인에서 백테스트가 동작하도록 시드 기반 난수로 가격을 생성한다.
실제 연구 시에는 load_csv 로 실데이터를 넣을 수 있다(컬럼: date,open,high,low,close,volume).
"""
from __future__ import annotations

import csv
import math
import random
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path


@dataclass(frozen=True)
class Bar:
    """일봉 한 개."""
    date: str
    open: float
    high: float
    low: float
    close: float
    volume: int


def generate_ohlcv(
    n: int = 500,
    seed: int = 42,
    start_price: float = 70000.0,
    mu: float = 0.0003,
    sigma: float = 0.015,
    start_date: date | None = None,
) -> list[Bar]:
    """기하 브라운 운동(GBM) 기반 합성 일봉. 같은 seed 면 항상 동일한 결과(재현성)."""
    rng = random.Random(seed)
    d = start_date or date(2023, 1, 2)
    price = start_price
    bars: list[Bar] = []
    for _ in range(n):
        # 일간 로그수익률 ~ N(mu, sigma)
        ret = mu + sigma * rng.gauss(0.0, 1.0)
        prev = price
        price = max(1.0, prev * math.exp(ret))
        o = prev
        c = price
        hi = max(o, c) * (1.0 + abs(rng.gauss(0.0, sigma / 3)))
        lo = min(o, c) * (1.0 - abs(rng.gauss(0.0, sigma / 3)))
        vol = int(abs(rng.gauss(1_000_000, 200_000)))
        # 주말 건너뛰기(영업일 근사)
        while d.weekday() >= 5:
            d += timedelta(days=1)
        bars.append(Bar(d.isoformat(), round(o, 1), round(hi, 1), round(lo, 1), round(c, 1), vol))
        d += timedelta(days=1)
    return bars


def load_csv(path: str | Path) -> list[Bar]:
    """CSV(date,open,high,low,close,volume) 로더."""
    bars: list[Bar] = []
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            bars.append(Bar(
                date=row["date"],
                open=float(row["open"]),
                high=float(row["high"]),
                low=float(row["low"]),
                close=float(row["close"]),
                volume=int(float(row.get("volume", 0))),
            ))
    return bars
