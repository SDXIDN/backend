# Лабораторна робота №6 — Quartz Scheduler

## Завдання (домен «бронювання»)

| Job | Що робить | Тригери |
|-----|-----------|---------|
| `BookingReminderJob` | надсилає нагадування про бронювання, що почнуться найближчим часом (`BookingReminderService`) | **2 тригери**: кожні 15 хв (використовує `hoursAhead=24` із Job) і щодня о 08:00 Europe/Kyiv (перевизначає `hoursAhead=48`) |
| `BookingCleanupJob` | скасовує PENDING-бронювання, які вже закінчились (`BookingCleanupService`) | cron щодня о 03:30 Europe/Kyiv |

Усе оголошено в одному класі `scheduling/QuartzConfig` (біни `JobDetail` і `Trigger`, тригери посилаються на завдання через `JobKey`, всі мають name і group).

## Збереження розкладу
- Quartz працює з `jdbc` job store (`spring.quartz.job-store-type: jdbc`).
- Таблиці `QRTZ_*` створює Liquibase: `db/changelog/changes/002-quartz-tables.yaml` + `002-quartz-tables.sql`.
- `spring.quartz.jdbc.initialize-schema: never` — Spring Boot таблиці не створює.
- `overwrite-existing-jobs: true` — конфігурація в коді є джерелом істини; повторний деплой оновлює завдання (за тим самим ключем) і не дублює їх.
- Для ідемпотентності додано колонку `bookings.reminder_sent` (`003-add-booking-reminder-sent.yaml`).

## JobDataMap
Значення за замовчуванням лежать у `JobDetail` (`hoursAhead=24`, `expireAfterMinutes=60`, `maxRetries=2`), тригер може перевизначити окремі ключі.
Quartz об'єднує мапи так: спочатку дані Job, поверх них дані тригера (`context.getMergedJobDataMap()`).
Відсутній ключ або опечатка → `IllegalArgumentException` (`JobParams.requireInt`), задокументованого значення за замовчуванням у коді Job немає.

## Безпечність і збої
- **Легкі Job:** прочитати параметри → один виклик сервісу; транзакція на методі сервісу (`@Transactional`).
- **`@DisallowConcurrentExecution`** на обох Job: наступний запуск чекає завершення попереднього.
- **Ідемпотентність:** нагадування позначає бронювання `reminder_sent=true`; cleanup — bulk-update лише PENDING-записів.
- **Стратегія збоїв (`JobFailurePolicy`):** повторний запуск з обмеженням `maxRetries` (через `context.getRefireCount()`), після вичерпання — помилка логується, чекаємо наступного тригера. Нескінченного циклу немає.
- **Misfire:** frequent — `RESCHEDULE_NEXT_WITH_REMAINING_COUNT`; daily reminder — `FIRE_ONCE_NOW`; cleanup — `DO_NOTHING`.
- **Часовий пояс:** cron-тригери явно мають `Europe/Kyiv` (через `ZoneId.of`, тож невідомий id дає помилку при старті, а не тихий GMT).

## Тести
- `QuartzConfigTest` — JobKey, name/group, два тригери, злиття JobDataMap (як у Quartz), misfire, часовий пояс, `@DisallowConcurrentExecution`.
- `BookingReminderJobTest`, `BookingCleanupJobTest` — легкість Job, помилка при невірних ключах, повтор при збої.
- `JobFailurePolicyTest` — ліміт повторів, відсутність нескінченного циклу.
- `BookingReminderServiceTest`, `BookingCleanupServiceTest` — повторний запуск нічого не змінює; збій одного листа не ламає решту.
- `QuartzPersistenceConfigTest` — jdbc store, `initialize-schema: never`, усі 11 таблиць QRTZ_* у Liquibase.

Запуск: `mvn test` (з папки `app`).

## Перевірка роботи
1. `mvn spring-boot:run` (Docker запущений).
2. У БД: `SELECT job_name, job_group FROM qrtz_job_details;` і `SELECT trigger_name, trigger_state FROM qrtz_triggers;` — мають бути 2 завдання і 3 тригери.
3. Перезапустіть застосунок — кількість записів не змінюється (дублів немає).
