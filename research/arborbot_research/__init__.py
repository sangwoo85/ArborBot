"""ArborBot 전략 연구·백테스트 모듈.

외부 데이터/무거운 의존성 없이 표준 라이브러리만으로 재현 가능한 백테스트를 제공한다.
STRATEGY_GOVERNANCE 의 성과 지표(누적수익률·MDD·승률·손익비·샤프·거래횟수·거래비용 반영)를 계산한다.
"""

__all__ = ["data", "strategy", "engine", "metrics"]
__version__ = "0.1.0"
