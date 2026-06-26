package com.tossai.trading.application.service.portfolio;

import com.tossai.trading.application.config.SettlementProperties;
import com.tossai.trading.application.port.out.AuditLogRepository;
import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.application.port.out.SettlementRepository;
import com.tossai.trading.common.util.Ids;
import com.tossai.trading.domain.audit.AuditLog;
import com.tossai.trading.domain.portfolio.SettlementLot;
import com.tossai.trading.domain.portfolio.TradingCalendar;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * T+2 결제 처리. 매수 체결 시 결제 로트를 기록하고, 결제일(영업일=주말+공휴일 제외) 도래분을
 * 매도가능수량으로 전환한다. 매수 직후에는 매도가 불가하며, 결제 이후에만 매도 가능해진다.
 *
 * <p>전량 매도로 보유가 사라지면 해당 종목의 미결제 로트를 정리한다(stale 로트가 이후 재매수
 * 포지션에 잘못 매도가능수량을 더하지 않도록).
 */
@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditLogRepository auditLogRepository;
    private final SettlementProperties props;

    public SettlementService(SettlementRepository settlementRepository,
                             PortfolioRepository portfolioRepository,
                             AuditLogRepository auditLogRepository,
                             SettlementProperties props) {
        this.settlementRepository = settlementRepository;
        this.portfolioRepository = portfolioRepository;
        this.auditLogRepository = auditLogRepository;
        this.props = props;
    }

    private TradingCalendar calendar() {
        return new TradingCalendar(props.holidaySet());
    }

    /** 매수 체결분을 결제 대기 로트로 기록(매도가능수량은 아직 늘지 않음). */
    public void recordBuy(String symbol, long quantity, LocalDate tradeDate) {
        if (quantity <= 0) {
            return;
        }
        LocalDate settleDate = calendar().addBusinessDays(tradeDate, props.getBusinessDays());
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

    /**
     * 매도 후 정리: 보유가 모두 사라진 종목의 미결제 로트(FIFO)를 제거한다.
     * 부분 매도로 보유가 남아 있으면 로트는 유지한다(결제 시 매도가능수량으로 전환, 보유 한도로 캡).
     */
    public void onSell(String symbol) {
        boolean stillHeld = portfolioRepository.getCurrent().findPosition(symbol) != null;
        if (!stillHeld) {
            settlementRepository.deleteUnsettledBySymbol(symbol);
        }
    }
}
