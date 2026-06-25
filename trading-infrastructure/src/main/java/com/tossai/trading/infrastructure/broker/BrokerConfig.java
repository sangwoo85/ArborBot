package com.tossai.trading.infrastructure.broker;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 브로커 연동 설정/빈 등록. RestClient 빌더에 연결/읽기 타임아웃을 적용한다. */
@Configuration
@EnableConfigurationProperties(BrokerProperties.class)
public class BrokerConfig {

    @Bean
    public RestClient.Builder brokerRestClientBuilder(BrokerProperties props) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
