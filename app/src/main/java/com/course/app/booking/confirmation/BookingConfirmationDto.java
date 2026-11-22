package com.course.app.booking.confirmation;

import java.util.List;

/**
 * Підсумковий результат двох незалежних асинхронних викликів (PDF + календар).
 * Якщо один з викликів не вдався або не вклався в тайм-аут, відповідне поле порожнє,
 * а причина потрапляє у warnings — запит не завершується помилкою.
 */
public record BookingConfirmationDto(
        Long bookingId,
        String documentName,
        Integer documentSizeBytes,
        String calendarEventId,
        boolean complete,
        List<String> warnings) {
}
