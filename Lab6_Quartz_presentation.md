---
marp: true
theme: default
paginate: true
title: Лабораторна робота №6 — Quartz Scheduler
---

# Лабораторна робота №6
## Quartz Scheduler: регулярні завдання за розкладом

Проєкт: **booking and scheduling service**
Пакет: `com.course.app.scheduling`

> Мета: щоб розгортання нової версії не призводило до непомітного скасування або дублювання регулярних завдань.

---

## Проблема

Регулярні завдання потрібно виконувати за розкладом, який **переживає перезапуск**:

| # | Завдання | Навіщо |
|---|----------|--------|
| 1 | Нагадування про бронювання | Клієнт отримує лист перед початком |
| 2 | Очищення прострочених бронювань | PENDING-записи, що так і не підтвердили, не засмічують БД |

Без постійного сховища розклад губиться при рестарті, а повторний деплой може задвоїти завдання.

---

## Архітектура рішення

```
QuartzConfig (@Configuration)
 ├─ JobDetail  bookingReminderJob ──┬─ Trigger: кожні 15 хв   (hoursAhead = 24)
 │                                  └─ Trigger: щодня 08:00   (hoursAhead = 48)
 └─ JobDetail  bookingCleanupJob ─────  Trigger: щодня 03:30

Job (легкий)  ──►  Spring-сервіс (@Transactional)  ──►  BookingRepository
                    ├─ BookingReminderService
                    └─ BookingCleanupService

Розклад і стан  ──►  PostgreSQL, таблиці QRTZ_* (створює Liquibase)
```

---

## Схема Quartz у Liquibase

- Схему взято зі стандартного `tables_postgres.sql` Quartz і адаптовано
- Changeset `002-quartz-tables` підключений у `db.changelog-master.yaml`
- Створюється **11 таблиць**: `QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, `QRTZ_CRON_TRIGGERS`, `QRTZ_SIMPLE_TRIGGERS`, `QRTZ_FIRED_TRIGGERS`, `QRTZ_LOCKS` та інші

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never   # таблиці створює Liquibase, не Spring Boot
    overwrite-existing-jobs: true
```

Повторний деплой оновлює завдання за тим самим ключем і **не дублює** їх.

---

## Конфігурація: усе в одному місці

```java
@Configuration
public class QuartzConfig {
    public static final String GROUP = "booking";
    public static final JobKey REMINDER_JOB_KEY =
            JobKey.jobKey("bookingReminderJob", GROUP);

    @Bean
    public JobDetail bookingReminderJobDetail() {
        return JobBuilder.newJob(BookingReminderJob.class)
                .withIdentity(REMINDER_JOB_KEY)
                .usingJobData("hoursAhead", 24)
                .usingJobData("maxRetries", 2)
                .storeDurably().build();
    }
    // + Trigger-біни, кожен через .forJob(JOB_KEY)
}
```

Кожне завдання й кожен тригер мають явні **name** і **group**.

---

## Два тригери для одного Job

```java
// Тригер 1: кожні 15 хв, параметри беруться з Job
SimpleScheduleBuilder.simpleSchedule()
    .withIntervalInMinutes(15).repeatForever()

// Тригер 2: щодня о 08:00 Europe/Kyiv, перевизначає hoursAhead
.usingJobData("hoursAhead", 48)
.withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 * * ?")
        .inTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Kyiv"))))
```

Один клас `BookingReminderJob` обслуговує обидва розклади з різними параметрами.

---

## Параметри через JobDataMap

Quartz об'єднує мапи: **спочатку дані Job, поверх них дані тригера**.

| Ключ | Job (за замовчуванням) | Тригер daily | Результат |
|------|------------------------|--------------|-----------|
| `hoursAhead` | 24 | **48** | 48, тригер має пріоритет |
| `maxRetries` | 2 | не задано | 2, лишається значення Job |

```java
JobDataMap data = context.getMergedJobDataMap();
int hoursAhead = JobParams.requireInt(data, "hoursAhead");
```

Відсутній ключ або опечатка → явний `IllegalArgumentException`, а не тихе значення за замовчуванням.

---

## «Легкі» Job

```java
@DisallowConcurrentExecution
public class BookingReminderJob implements Job {
    @Autowired private BookingReminderService reminderService;

    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        int hoursAhead = JobParams.requireInt(ctx.getMergedJobDataMap(), KEY_HOURS_AHEAD);
        int maxRetries = JobParams.requireInt(ctx.getMergedJobDataMap(), KEY_MAX_RETRIES);
        try {
            reminderService.sendUpcomingReminders(hoursAhead);   // один виклик сервісу
        } catch (RuntimeException ex) {
            JobFailurePolicy.handle(BookingReminderJob.class, ctx, ex, maxRetries);
        }
    }
}
```

- Бізнес-логіка лише в сервісі
- Межа транзакції проходить на методі сервісу (`@Transactional`)

---

## Паралельний запуск заборонено

`@DisallowConcurrentExecution` на обох Job.

- Якщо наступний запуск настав, а попередній ще триває, він **чекає завершення**
- Анотація діє на рівні `JobDetail`, тому два тригери одного Job теж не виконаються одночасно
- Для нагадувань це критично: два паралельні запуски не надішлють один лист двічі

---

## Ідемпотентність

| Завдання | Механізм |
|----------|----------|
| Нагадування | Після відправки `reminder_sent = true` (changeset `003`), вибірка бере лише `reminder_sent = false` |
| Очищення | Bulk-update змінює лише записи у статусі `PENDING`, повторний запуск нічого не знаходить |

Повторний запуск одразу після першого **не змінює результат**.

Збій одного листа не відкочує вже оброблені бронювання: невдалий лист піде при наступному запуску.

---

## Стратегія збоїв

Єдина політика `JobFailurePolicy`: **повторний запуск з обмеженням спроб**.

```java
int attempt = context.getRefireCount();
if (attempt < maxRetries) {
    throw new JobExecutionException(failure, true);   // refireImmediately
}
log.error("... failed after {} retries, giving up until the next scheduled run");
```

- Лічильник бере сам Quartz (`getRefireCount()`)
- Після вичерпання спроб помилка лише логується, завдання чекає наступного тригера
- **Нескінченного циклу немає**

---

## Misfire і часовий пояс

| Тригер | Misfire-інструкція |
|--------|--------------------|
| frequent (15 хв) | `RESCHEDULE_NEXT_WITH_REMAINING_COUNT` |
| daily reminder | `FIRE_ONCE_NOW` |
| cleanup | `DO_NOTHING` |

- Для кожного тригера інструкція задана **явно**, тест її фіксує
- Cron-тригери явно мають `Europe/Kyiv` і не залежать від часового поясу сервера
- `ZoneId.of(...)` кидає помилку на невідомий id, а не тихо підставляє GMT

---

## Відповідність сценаріям лабораторної

| Сценарій | Як перевірено |
|----------|---------------|
| Job і тригери в одному `@Configuration`, зв'язок через `JobKey`, name і group | `QuartzConfigTest` |
| Параметри тригера мають пріоритет | `mergedJobDataMap_triggerValueWins...` |
| Job «легкі»: один виклик сервісу | `BookingReminderJobTest`, `BookingCleanupJobTest` |
| Помилка при невірних параметрах | `execute_missingKey...`, `execute_misspelledKey...`, `execute_wrongValueType...` |
| `@DisallowConcurrentExecution` | `jobs_disallowConcurrentExecution` |
| Явний misfire | `everyTrigger_hasExplicitMisfireInstruction` |
| Часовий пояс cron | `cronTriggers_haveExplicitTimeZoneNotServerDefault` |
| Повторний запуск безпечний | `calledTwiceForSameData_secondRunChangesNothing` |
| Збій обмежений, без нескінченного циклу | `JobFailurePolicyTest` |
| Розклад у БД, `initialize-schema: never` | `QuartzPersistenceConfigTest` |

---

## Тестування

- **Юніт-тести Job**: `BookingReminderJobTest`, `BookingCleanupJobTest`, `JobFailurePolicyTest`
- **Тести конфігурації**: `QuartzConfigTest` (злиття `JobDataMap` так, як це робить Quartz), `QuartzPersistenceConfigTest`
- **Тести сервісів**: `BookingReminderServiceTest`, `BookingCleanupServiceTest`: ідемпотентність, збій одного листа не ламає решту

Запуск: `mvn test` (з папки `app`).

---

## Демонстрація

1. `docker compose up -d`, потім `mvn spring-boot:run`
2. Перевірити розклад у БД:
   ```sql
   SELECT job_name, job_group FROM qrtz_job_details;      -- 2 завдання
   SELECT trigger_name, trigger_state FROM qrtz_triggers; -- 3 тригери
   ```
3. Перезапустити застосунок: кількість записів **не змінюється** (дублів немає)
4. У логах кожні 15 хв: `Booking reminders: N sent out of M due`
5. UI: http://localhost:8080/ui/bookings (`user` / `user`)

---

## Висновки

- Розклад зберігається в **PostgreSQL** (JDBC job store), схему веде **Liquibase**
- Два Job і три тригери оголошені в одному `@Configuration`; параметри передаються через `JobDataMap` із перевизначенням у тригері
- Завдання **ідемпотентні**, не виконуються паралельно, мають явний misfire і часовий пояс
- Збої обробляються передбачувано: обмежений повтор, потім очікування наступного запуску

**Дякую за увагу!**
