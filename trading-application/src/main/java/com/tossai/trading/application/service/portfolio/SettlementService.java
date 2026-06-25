package com.tossai.trading.application.service.portfolio;

import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.application.port.out.SettlementRepository;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.portfolio.SettlementLot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * T+2 결제 처리. 매수 체결 시 결제 로트를 기록하고, 결제일 도래분을 매도가능수량으로 전환한다.
 * 매수 직후에는 매도가 불가하며, 결제(T+2) 이후에만 매도 가능해진다.
 */
@Service
public class SettlementService {

    /** 결제 주기(영업일). 한국 주식 현물 기준 T+2. */
    private static final int SETTLEMENT_BUSINESS_DAYS = 2;

    private final SettlementRepository settlementRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditLogRepository auditLogRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             PortfolioRepository portfolioRepository,
                             AuditLogRepository auditLogRepository) {
        this.settlementRepository = settlementRepository;
        this.portfolioRepository = portfolioRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** 매수 체결분을 결제 대기 로트로 기록(매도가능수량은 아직 늘지 않음). */
    public void recordBuy(String symbol, long quantity, LocalDate tradeDate) {
        if (quantity <= 0) {
            return;
        }
        LocalDate settleDate = SettlementLot.settlementDate(tradeDate, SETTLEMENT_BUSINESS_DAYS);
        settlementRepository.save(new SettlementLot(
                Ids.newId("lot"), symbol, quantity, settleDate, false));
    }

    /** 결제일 도래(<= asOf) 로트를 매도가능수량으로 전환. 반환: 처리한 로트 수. */
    public int settleDue(LocalDate asOf) {
        List<SettlementLot> due = settlementRepository.findDue(asOf);
        for (SettlementLot lot : due) {
            portfolioRepository.increaseSellable(lot.symbol(), lot.quantity());
            settlementRepository.markSettled(lot.lotId());
            auditLogRepository.save(new AuditLog(Ids.newId("audit"), null, "SETTLEMENT", "SETTLED",
                    "symbol=" + lot.symbol() + " qty=" + lot.quantity() + " settleDate=" + lot.settleDate(),
                    "batch", Instant.now()));
        }
        return due.size();
    }
}
