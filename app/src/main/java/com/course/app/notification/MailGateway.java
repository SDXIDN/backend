package com.course.app.notification;

/**
 * Межа з зовнішньою поштовою системою (SMTP / SaaS). Повільна, тому викликається асинхронно.
 */
public interface MailGateway {

    void send(String to, String subject, String body);
}
