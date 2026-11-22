---
marp: true
theme: default
paginate: true
title: Лабораторна робота №5 — Асинхронна обробка
---

# Лабораторна робота №5
## Асинхронна обробка в Spring Boot

Проєкт: **booking and scheduling service**
Пакет: `com.course.app`

> Мета: щоб «асинхронність» не означала «непомітні збої» чи «вичерпання ресурсів сервера».

---

## Проблема

Три повільні побічні ефекти блокували викликаючий код (~1 с кожен):

| # | Операція | Де |
|---|----------|----|
| 1 | Лист-підтвердження після створення бронювання | `NotificationService` |
| 2 | Генерація PDF-підтвердження | `DocumentService` |
| 3 | Виклик зовнішнього календаря | `CalendarSyncService` |

Раніше все виконувалося в потоці HTTP-запиту.

---

## Архітектура рішення

```
POST /api/v1/bookings
   └─ BookingService.create ──(подія BookingCreatedEvent)──┐
                                                           ▼
                                   AFTER_COMMIT: BookingCreatedListener
                                                           ▼
                                  NotificationService (@Async void)  ← fire-and-forget

POST /api/v1/bookings/{id}/confirmation
   └─ BookingConfirmationService
        ├─ DocumentService.generateConfirmationPdf   (@Async → CompletableFuture)
        └─ CalendarSyncService.syncToExternalCalendar (@Async → CompletableFuture)
              └─ thenCombine → BookingConfirmationDto
```

Усі `@Async` працюють у власному пулі `bookingTaskExecutor`.

---

## Власний виконавець (ThreadPoolTaskExecutor)

```java
@Bean(name = BOOKING_EXECUTOR)
public ThreadPoolTaskExecutor bookingTaskExecutor(...) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("booking-async-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return executor;
}
```

- Розміри налаштовуються в `application.yml` (`app.async.*`)
- Кожен `@Async` явно вказує `BOOKING_EXECUTOR`
- Не `SimpleAsyncTaskExecutor` і не пул за замовчуванням

---

## Fire-and-forget: лист

```java
@Async(AsyncConfig.BOOKING_EXECUTOR)
public void sendBookingCreatedEmail(BookingDto booking) {
    mailGateway.send(...);
}
```

- Викликається після **коміту** транзакції (`@TransactionalEventListener(AFTER_COMMIT)`)
- Відповідь клієнту приходить одразу
- Помилка `void`-методу → `LoggingAsyncExceptionHandler` (лог + лічильник), а не мовчазна втрата

---

## Результат потрібен: два паралельні виклики

```java
CompletableFuture<GeneratedDocument> pdf = documentService.generateConfirmationPdf(booking);
CompletableFuture<String> calendar = calendarSyncService.syncToExternalCalendar(booking);

return guarded(pdf, "PDF generation")
        .thenCombine(guarded(calendar, "Calendar sync"), (p, c) -> assemble(bookingId, p, c))
        .join();
```

- Обидва виклики стартують, не чекаючи один на одного
- Час запиту ≈ 1 с замість ≈ 2 с

---

## Обробка збоїв і тайм-аут

```java
future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
      .handle((value, ex) -> ex == null
              ? Outcome.success(value)
              : Outcome.failure(operation + " failed / timed out"));
```

- Збій одного виклику → **часткова відповідь** із `warnings`, запит не падає
- Кожне очікування обмежене тайм-аутом (3 с) — `join()` не може зависнути
- Приклад відповіді: `complete=false`, `warnings=["Calendar sync timed out"]`

---

## Переповнення виконавця

Пул заповнений → `TaskRejectedException`.

**Рішення:** чітка відповідь «спробуйте пізніше», а не випадкова 500.

```java
catch (TaskRejectedException ex) {
    throw new ServiceBusyException("Async executor is saturated", ex);
}
```

```java
@ExceptionHandler({ServiceBusyException.class, TaskRejectedException.class})
→ HTTP 503 + заголовок Retry-After: 5
```

Лист — best-effort: при переповненні пропускається з warning у логах (бронювання вже збережене).

---

## Відповідність сценаріям лабораторної

| Сценарій | Як перевірено |
|----------|---------------|
| Повільний метод не блокує виклик | `sendEmail_slowGateway_returnsBefore...` (CountDownLatch) |
| Явний обмежений виконавець, префікс потоку | `executor_isExplicitlyConfigured...`, перевірка `booking-async-` |
| Два виклики виконуються одночасно | `confirm_twoIndependentCalls_runConcurrently` (< 900 мс замість ≥ 1000) |
| Збій не валить виклик (`exceptionally`/`handle`) | `confirm_calendarFails_returnsPartialResult...` |
| `void`-помилка фіксується обробником | `sendEmail_gatewayThrows_...HandlerRecordsFailure` |
| Очікування обмежене в часі | `confirm_pdfNeverCompletes_stopsWaitingAfterTimeout` |
| Переповнення → не 500 | `ExecutorOverflowIntegrationTest`, `GlobalExceptionHandlerTest` (503) |

---

## Тестування

- **Інтеграційні** (`@SpringJUnitConfig`): `AsyncExecutionIntegrationTest`, `ExecutorOverflowIntegrationTest`
  - мінімальний пул: 1 потік, черга 0 → відхилення завдання
- **Юніт** (Mockito): `BookingConfirmationServiceTest`, `BookingCreatedListenerTest`, `BookingServiceEventTest`, `BookingConfirmationControllerTest`
- Детермінованість: затримки через `CountDownLatch`, а не лише `sleep`

Запуск: `mvn test` (з папки `app`), поріг покриття 60% — `mvn verify`.

---

## Демонстрація

1. Створити бронювання → відповідь **миттєва**
2. У логу через ≈ 1 с: `Email sent ...` у потоці `booking-async-1`
3. `POST /api/v1/bookings/{id}/confirmation` → ≈ 1 с, `complete: true`
4. Затримка 5 с → тайм-аут 3 с, `warnings` у відповіді
5. Пул 1 потік / черга 0 → **503** `Service is busy, please try again later`

---

## Висновки

- Повільні операції винесені з потоку запиту у **власний обмежений пул**
- **Fire-and-forget** і **об'єднання результатів** (`thenCombine`) реалізовані на прикладі домену бронювань
- Збої, тайм-аути й переповнення обробляються **передбачувано**: лог, часткова відповідь, HTTP 503
- Усі сценарії лабораторної покриті автоматичними тестами

**Дякую за увагу!**
