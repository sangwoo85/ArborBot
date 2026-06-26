package com.tossai.trading.application.port.out;

import com.tossai.trading.domain.portfolio.SettlementLot;

import java.time.LocalDate;
import java.util.List;

/** 매수 결제 로트 저장소. T+2 결제 처리에 사용. */
public interface SettlementRepository {
    void save(SettlementLot lot);

    /** 결제일이 도래(<= asOf)했고 아직 미결제인 로트. */
    List<SettlementLot> findDue(LocalDate asOf);

    void markSettled(String lotId);

    /** 종목의 미결제 로트(결제일 오름차순, FIFO). */
    List<SettlementLot> findUnsettledBySymbol(String symbol);

    /** 종목의 미결제 로트 전체 삭제(전량 매도로 보유가 사라진 경우 정리). */
    void deleteUnsettledBySymbol(String symbol);
}
