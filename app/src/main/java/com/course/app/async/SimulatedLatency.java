package com.course.app.async;

/**
 * Імітація повільного зовнішнього виклику (SMTP, рендер PDF, HTTP-запит до календаря).
 * У реальному проєкті замість цього був би справжній виклик.
 */
public final class SimulatedLatency {

    private SimulatedLatency() {
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating a slow call", e);
        }
    }
}
