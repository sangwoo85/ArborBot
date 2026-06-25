package com.tossai.trading.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("toss-ai-trading-platform API")
                .version("0.1.0")
                .description("AI 기반 개인용 자동매매 플랫폼 관리자 API (Mock 기반). "
                        + "AI 는 신호만 생성하고, 주문은 Risk Engine 검증을 통과한 경우에만 실행된다."));
    }
}
