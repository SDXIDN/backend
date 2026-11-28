# Лабораторна робота №4

**Тема:** AOP (Aspect Oriented Programming)  
**Дисципліна:** Розробка високонавантажених корпоративних систем  
**Виконав:** ПІБ, група  
**Проєкт:** `backend-template` — сервіс бронювання (Booking & Scheduling). Java 21, Spring Boot 3.5.16, PostgreSQL 16, Liquibase, MapStruct, Keycloak 26.

## Мета
Реалізувати наскрізну функціональність у вигляді пар «власна анотація + аспект», застосувати їх у робочому коді та навмисно продемонструвати проблему self-invocation.

## Реалізовані пари «анотація + аспект»
Усі анотації мають `@Retention(RUNTIME)` та `@Target(METHOD)`; усі аспекти — `@Aspect @Component` з advice типу `@Around` і pointcut на основі `@annotation(...)`. Застосовано до методів `BookingService` (залежність `spring-boot-starter-aop`).

| Анотація | Аспект | Concern | Що відбувається | Застосування |
| :--- | :--- | :--- | :--- | :--- |
| `@RateLimited(maxCalls, perSeconds)` | `RateLimitAspect` | обмеження частоти викликів | після перевищення ліміту кидає `TooManyRequestsException` ($\rightarrow$ HTTP 429), `proceed()` не викликається | `BookingService.create` (10 викликів / 60 с) |
| `@Audited(action)` | `AuditAspect` | аудит дій | пише `AuditEntry` (хто, що, коли, `SUCCESS`/`FAILURE`) в `AuditLog` і лог `AUDIT`, виняток перекидається далі | `BookingService.update`, `delete` |
| `@MaskEmail` | `MaskEmailAspect` | маскування персональних даних | змінює значення, що повертається: `john.smith@example.com` $\rightarrow$ `j*********@example.com` | `BookingService.findAll` |

Це реальні cross-cutting задачі (rate limiting, аудит, захист персональних даних), а не копії логування чи контролю доступу з лекції.

### `RateLimitAspect` (кидає виняток)

```java
@Around("@annotation(rateLimited)")
public Object limit(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
    String key = pjp.getSignature().toShortString() + "|" + CurrentActor.name();
    Instant now = clock.instant();
    Instant windowStart = now.minusSeconds(rateLimited.perSeconds());
    Deque<Instant> window = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (window) {
        while (!window.isEmpty() && !window.peekFirst().isAfter(windowStart)) {
            window.pollFirst();
        }
        if (window.size() >= rateLimited.maxCalls()) {
            throw new TooManyRequestsException("Rate limit exceeded ...");
        }
        window.addLast(now);
    }
    return pjp.proceed();
}
```
Ліміт рахується окремо для кожного користувача й методу (ковзне вікно); час береться з `Clock`, що дозволяє тестувати без `sleep`.

### `AuditAspect` (запис у лог)

```java
@Around("@annotation(audited)")
public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
    try {
        Object result = pjp.proceed();
        record(audited, target, "SUCCESS", "ids=[" + ids + "]");
        return result;
    } catch (Throwable ex) {
        record(audited, target, "FAILURE", "ids=[" + ids + "] error=" + ex.getClass().getSimpleName());
        throw ex;
    }
}
```
У журнал потрапляють лише числові аргументи (`id`), тому персональні дані (email тощо) в аудит не пишуться.

### `MaskEmailAspect` (змінює значення, що повертається)

```java
@Around("@annotation(com.course.app.aop.MaskEmail)")
public Object mask(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();
    if (result instanceof BookingDto dto) {
        maskDto(dto);
    } else if (result instanceof Collection<?> items) {
        for (Object item : items) {
            if (item instanceof BookingDto dto) maskDto(dto);
        }
    }
    return result;
}
```

## Демонстрація self-invocation
Клас `SelfInvocationDemo` містить анотований метод і два шляхи його виклику:

```java
@Component
public class SelfInvocationDemo {

    @MaskEmail
    public BookingDto sampleBooking() {
        BookingDto dto = new BookingDto();
        dto.setCustomerEmail("john.smith@example.com");
        return dto;
    }

    public BookingDto sampleBookingViaThis() {
        return this.sampleBooking();   // self-invocation: проксі оминається
    }
}
```

Контролер `AopDemoController` викликає обидва методи через ін'єктований бін (тобто через проксі) за адресою `GET /api/public/aop-demo`:

| Шлях виклику | Що відбувається | Email у відповіді |
| :--- | :--- | :--- |
| `demo.sampleBooking()` — ззовні, через проксі | advice спрацьовує | `j*********@example.com` |
| `demo.sampleBookingViaThis()` $\rightarrow$ `this.sampleBooking()` | виклик усередині класу йде повз проксі, advice не спрацьовує, помилки немає | `john.smith@example.com` |

**Причина:** Spring AOP працює на проксі-об'єктах; зовнішній виклик проходить через проксі, а `this.method()` викликає метод цільового об'єкта напряму. Така відсутність спрацювання — свідомий задум вправи, а не помилка.

## Сценарії та тести

| Сценарій | Підтвердження |
| :--- | :--- |
| Анотація правильно пов'язана з аспектом (`RUNTIME`, `METHOD`, `@annotation(...)`) | `AopWiringAndSelfInvocationTest`: `customAnnotations_haveRuntimeRetentionAndMethodTarget`, `aspects_usePointcutBasedOnTheirAnnotation`, `annotationsAreActuallyPresentOnRealBookingServiceMethods` |
| Поведінка advice помітна при зовнішньому виклику через проксі | `externalCall_throughProxy_aspectRuns` |
| Виклик з іншого бін-об'єкта / напряму з тесту активує аспект | `externalCall_onRealBookingService_masksEmailsOfReturnedBookings` (реальний `BookingService` у `AspectJProxyFactory`) |
| Self-invocation НЕ активує аспект | `selfInvocation_viaThis_aspectIsSilentlySkipped`, `sameAnnotatedMethod_twoCallPaths_giveDifferentResults` |
| Unit-тест advice з mock `ProceedingJoinPoint`, без Spring-контексту | `MaskEmailAspectTest`, `AuditAspectTest`, `RateLimitAspectTest` |

У unit-тестах аспектів `ProceedingJoinPoint` створюється через `mock(...)` (без `@SpringBootTest`), а перевіряється саме логіка прийняття рішень в advice:
* `RateLimitAspectTest.limit_overLimit_throwsAndDoesNotProceed` та `limit_overLimit_neverTouchesProceed` — після перевищення ліміту `proceed()` не викликається (`verify(pjp, never()).proceed()`);
* `limit_afterWindowPasses_allowsAgain`, `limit_differentUsers_haveIndependentLimits` — вікно часу та незалежність користувачів;
* `AuditAspectTest.audit_success_proceedsReturnsResultAndRecordsSuccess` — `proceed()` викликається рівно один раз, результат повертається без змін, запис `SUCCESS`;
* `audit_methodThrows_recordsFailureAndRethrowsSameException` — запис `FAILURE` і той самий виняток;
* `MaskEmailAspectTest.mask_singleDto_…`, `mask_listOfDtos_masksEveryElement`, `mask_nullResult_returnsNull`, `mask_methodThrows_exceptionPropagates`.

## Висновок
Реалізовано три пари «власна анотація + аспект» для реальних наскрізних задач: обмеження частоти викликів, аудит та маскування персональних даних. Аспекти застосовані до методів `BookingService`. Self-invocation продемонстровано навмисно (`SelfInvocationDemo`, ендпоінт `/api/public/aop-demo`) і підтверджено тестами, а логіку advice перевірено unit-тестами з mock-об'єктом `ProceedingJoinPoint` без Spring-контексту.