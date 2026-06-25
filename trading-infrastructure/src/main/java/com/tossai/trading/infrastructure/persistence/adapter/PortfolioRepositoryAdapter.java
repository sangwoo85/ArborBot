package com.tossai.trading.infrastructure.persistence.adapter;

import com.tossai.trading.application.port.out.PortfolioRepository;
import com.tossai.trading.domain.portfolio.Portfolio;
import com.tossai.trading.domain.portfolio.Position;
import com.tossai.trading.infrastructure.persistence.entity.AccountBalanceEntity;
import com.tossai.trading.infrastructure.persistence.entity.PositionEntity;
import com.tossai.trading.infrastructure.persistence.jpa.AccountBalanceJpaRepository;
import com.tossai.trading.infrastructure.persistence.jpa.PositionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
public class PortfolioRepositoryAdapter implements PortfolioRepository {

    private static final String ACCOUNT_ID = "ACC-DEMO";

    private final AccountBalanceJpaRepository balanceJpa;
    private final PositionJpaRepository positionJpa;

    public PortfolioRepositoryAdapter(AccountBalanceJpaRepository balanceJpa,
                                      PositionJpaRepository positionJpa) {
        this.balanceJpa = balanceJpa;
        this.positionJpa = positionJpa;
    }

    @Override
    public Portfolio getCurrent() {
        AccountBalanceEntity bal = balanceJpa.findById(ACCOUNT_ID)
                .orElseGet(() -> {
                    AccountBalanceEntity e = new AccountBalanceEntity();
                    e.setAccountId(ACCOUNT_ID);
                    e.setCash(BigDecimal.ZERO);
                    e.setOrderableAmount(BigDecimal.ZERO);
                    return e;
                });
        List<Position> positions = positionJpa.findAll().stream().map(p -> new Position(
                p.getSymbol(), p.getSector(), p.getQuantity(), p.getSellableQuantity(),
                p.getAvgPrice(), p.getLastPrice(), p.getEvaluationAmount(), p.getUnrealizedPnl()
        )).toList();
        return new Portfolio(ACCOUNT_ID, bal.getCash(), bal.getOrderableAmount(), positions);
    }

    @Override
    @Transactional
    public void applyBuy(String symbol, String sector, long quantity, BigDecimal price) {
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
        AccountBalanceEntity bal = balance();
        bal.setCash(bal.getCash().subtract(amount));
        bal.setOrderableAmount(bal.getOrderableAmount().subtract(amount));
        balanceJpa.save(bal);

        PositionEntity p = positionJpa.findById(symbol).orElse(null);
        if (p == null) {
            p = new PositionEntity();
            p.setSymbol(symbol);
            p.setSector(sector);
            p.setQuantity(quantity);
            p.setSellableQuantity(quantity);
            p.setAvgPrice(price);
        } else {
            long newQty = p.getQuantity() + quantity;
            BigDecimal newAvg = p.getAvgPrice().multiply(BigDecimal.valueOf(p.getQuantity()))
                    .add(amount)
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            p.setQuantity(newQty);
            p.setSellableQuantity(p.getSellableQuantity() + quantity);
            p.setAvgPrice(newAvg);
        }
        p.setLastPrice(price);
        p.setEvaluationAmount(price.multiply(BigDecimal.valueOf(p.getQuantity())));
        p.setUnrealizedPnl(price.subtract(p.getAvgPrice()).multiply(BigDecimal.valueOf(p.getQuantity())));
        positionJpa.save(p);
    }

    @Override
    @Transactional
    public void applySell(String symbol, long quantity, BigDecimal price) {
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
        AccountBalanceEntity bal = balance();
        bal.setCash(bal.getCash().add(amount));
        bal.setOrderableAmount(bal.getOrderableAmount().add(amount));
        balanceJpa.save(bal);

        PositionEntity p = positionJpa.findById(symbol).orElse(null);
        if (p == null) {
            return;
        }
        long newQty = Math.max(0, p.getQuantity() - quantity);
        if (newQty == 0) {
            positionJpa.delete(p);
            return;
        }
        p.setQuantity(newQty);
        p.setSellableQuantity(Math.max(0, p.getSellableQuantity() - quantity));
        p.setLastPrice(price);
        p.setEvaluationAmount(price.multiply(BigDecimal.valueOf(newQty)));
        p.setUnrealizedPnl(price.subtract(p.getAvgPrice()).multiply(BigDecimal.valueOf(newQty)));
        positionJpa.save(p);
    }

    private AccountBalanceEntity balance() {
        return balanceJpa.findById(ACCOUNT_ID).orElseGet(() -> {
            AccountBalanceEntity e = new AccountBalanceEntity();
            e.setAccountId(ACCOUNT_ID);
            e.setCash(BigDecimal.ZERO);
            e.setOrderableAmount(BigDecimal.ZERO);
            return e;
        });
    }
}
