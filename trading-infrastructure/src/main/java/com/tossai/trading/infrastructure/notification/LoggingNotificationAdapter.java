package com.tossai.trading.infrastructure.notification;

import com.tossai.trading.application.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 알림 어댑터(Mock). 실제로는 메신저/푸시로 발송한다.
 * 로그에는 비밀정보를 포함하지 않는다.
 */
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger("NOTIFICATION");

    @Override
    public void notify(String severity, String title, String message, String correlationId) {
        log.info("[ALERT][{}] {} | {} | correlationId={}", severity, title, message, correlationId);
    }
}
