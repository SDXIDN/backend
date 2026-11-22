package com.course.app.notification;

import com.course.app.async.SimulatedLatency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingMailGateway implements MailGateway {

    private final long delayMs;

    public LoggingMailGateway(@Value("${app.simulated-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimulatedLatency.sleep(delayMs);
        log.info("Email sent (subject='{}')", subject);
    }
}
