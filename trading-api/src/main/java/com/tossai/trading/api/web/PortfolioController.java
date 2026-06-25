package com.tossai.trading.api.web;

import com.tossai.trading.application.service.portfolio.PortfolioService;
import com.tossai.trading.domain.portfolio.Portfolio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public Portfolio get() {
        return portfolioService.getPortfolio();
    }
}
