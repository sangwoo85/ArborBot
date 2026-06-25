package com.tossai.trading.application.port.out;

public interface NotificationPort {
    void notify(String severity, String title, String message, String correlationId);
}
