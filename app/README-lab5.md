# Лабораторна робота №5 — Асинхронна обробка

## Три повільні побічні ефекти (домен «бронювання»)

| # | Операція | Клас | Патерн |
|---|----------|------|--------|
| 1 | Лист-підтвердження після створення бронювання | `NotificationService.sendBookingCreatedEmail` (`@Async void`) | fire-and-forget |
| 2 | Генерація PDF-підтвердження | `DocumentService.generateConfirmationPdf` (`@Async CompletableFuture<GeneratedDocument>`) | результат потрібен |
| 3 | Синхронізація із зовнішнім календарем | `CalendarSyncService.syncToExternalCalendar` (`@Async CompletableFuture<String>`) | результат потрібен |

`POST /api/v1/bookings/{id}/confirmation` запускає #2 і #3 одночасно, об'єднує їх через `thenCombine`
(`BookingConfirmationService`). Лист (#1) надсилається після коміту створення бронювання
(`BookingService.create` → `BookingCreatedEvent` → `@TransactionalEventListener(AFTER_COMMIT)`).

## Виконавець
`AsyncConfig.bookingTaskExecutor` — `ThreadPoolTaskExecutor`: core=2, max=4, queue=20, префікс `booking-async-`,
`AbortPolicy`. Налаштування — `app.async.*` в `application.yml`. Усі `@Async` явно вказують цей виконавець.

## Рішення щодо помилок / переповнення
- `void` @Async: `LoggingAsyncExceptionHandler` (лог + лічильник).
- `CompletableFuture`: `orTimeout` + `handle` → часткова відповідь із `warnings`, запит не падає.
- Очікування завжди обмежене: `app.async.confirmation-timeout-ms` (3000 мс) на кожен Future.
- `TaskRejectedException`: у confirm → `ServiceBusyException` → HTTP **503** + `Retry-After` (не 500);
  у листі — best-effort: пропуск з warning у логах (бронювання вже збережене).

## Тести (відповідність сценаріям)
- `AsyncExecutionIntegrationTest` — не блокує викликаючий код, префікс потоку, явний пул, паралельність, void-помилки.
- `ExecutorOverflowIntegrationTest` — мінімальний пул (1 потік, черга 0) → відхилення → `ServiceBusyException`.
- `BookingConfirmationServiceTest` — thenCombine, збій одного виклику, тайм-аут, відхилення.
- `GlobalExceptionHandlerTest` — 503 замість 500.
- `BookingCreatedListenerTest`, `BookingServiceEventTest`, `BookingConfirmationControllerTest`.

Запуск: `mvn test`. Ручна перевірка: створіть бронювання (`POST /api/v1/bookings`) — відповідь приходить одразу,
у логах за ~1 с з'являється `Email sent` у потоці `booking-async-1`; потім `POST /api/v1/bookings/{id}/confirmation`
повертається за ~1 с (а не за ~2 с).
