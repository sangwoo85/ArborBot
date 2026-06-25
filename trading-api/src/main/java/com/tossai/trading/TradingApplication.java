package com.tossai.trading;

import com.tossai.trading.application.config.TradingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 부트 진입점. 루트 패키지(com.tossai.trading)에서 컴포넌트/엔티티/리포지토리를 스캔하므로
 * application/infrastructure 모듈의 빈이 모두 등록된다.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TradingProperties.class)
public class TradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
    }
}
