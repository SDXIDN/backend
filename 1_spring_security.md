# Лабораторна робота №1

**Тема:** Spring Security  
**Дисципліна:** Розробка високонавантажених корпоративних систем  
**Виконав:** ПІБ, група  
**Проєкт:** `backend-template` — сервіс бронювання (Booking & Scheduling). Java 21, Spring Boot 3.5.16, PostgreSQL 16, Liquibase, MapStruct, Keycloak 26.

## Мета
Закласти технічний фундамент системи: базовий CUD-застосунок, базу даних під контролем міграцій, простий UI та подвійну модель автентифікації (OIDC для UI + JWT для API) з обмеженням доступу до ендпоінтів двома способами.

## Предметна область
Обрана предметна область — бронювання послуг. Основна сутність — `Booking` (назва події, email клієнта, час початку та завершення, статус `PENDING` / `CONFIRMED` / `CANCELLED`). Така область дозволяє надалі розвивати систему в багатомодульну (клієнти, ресурси, платежі, сповіщення).

Інфраструктура описана в `compose.yaml`: контейнери PostgreSQL (порт 5432) і Keycloak (порт 8081). Завдяки `spring-boot-docker-compose` вони запускаються автоматично разом із застосунком (`mvn spring-boot:run`).

## Хід виконання

### 1. CUD-застосунок і MapStruct (AC1)
Шарова архітектура: `BookingController` $\rightarrow$ `BookingService` $\rightarrow$ `BookingRepository` (`JpaRepository<BookingEntity, Long>`). Контролер працює з DTO, сервіс — з Entity, перетворення виконує MapStruct:

```java
@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingDto toDto(BookingEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    BookingEntity toEntity(CreateBookingRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(CreateBookingRequest request, @MappingTarget BookingEntity entity);
}
```

**REST API `/api/v1/bookings`:**

| Операція | Метод і шлях | Доступ | Успішний код |
| :--- | :--- | :--- | :--- |
| Create | `POST /api/v1/bookings` | USER | 201 |
| Read | `GET /api/v1/bookings` | USER, ADMIN | 200 |
| Update | `PUT /api/v1/bookings/{id}` | ADMIN | 200 |
| Delete | `DELETE /api/v1/bookings/{id}` | ADMIN | 204 |

Помилки обробляє `GlobalExceptionHandler` (`@RestControllerAdvice`) і повертає єдиний формат `ApiError`: `400` — валідація, `404` — `EntityNotFoundException`, `403`, `429`, `500`.

### 2. Міграції Liquibase (AC2)
Схема БД не створюється вручну: у `application.yml` встановлено `spring.jpa.hibernate.ddl-auto: validate`, а таблицю створює Liquibase при старті на чистій БД. Майстер-файл `db/changelog/db.changelog-master.yaml` підключає `changes/001-create-bookings.yaml` із двома changeSet:
* `001-create-bookings` — `createTable bookings` (`id` BIGINT autoIncrement PK, `title`, `customer_email`, `start_time`, `end_time`, `status` — усі `NOT NULL`);
* `002-seed-bookings` — початковий запис «Demo booking».

### 3. UI (AC3)
* `/ui/index.html` — статична сторінка з повним CUD: форма створення/редагування, таблиця з кнопками Edit/Delete, кнопки для виклику захищених ендпоінтів. JS отримує access-токен із `/ui/token` (`TokenController`) і викликає API із заголовком `Authorization: Bearer`.
* `/ui/bookings` — Thymeleaf-сторінка (`bookings.html`, Bootstrap) зі створенням і списком бронювань (`BookingUIController`).

### 4. OIDC для UI-користувачів (AC4)
Другий `SecurityFilterChain` (`@Order(2)`) обробляє все, окрім `/api/**`, працює із сесією та використовує `oauth2Login`. Шлях `/ui/**` вимагає автентифікації, тому неавтентифікований користувач редиректиться на сторінку логіну Keycloak (realm `course`, client `booking-app`), а після успіху повертається на `/ui/index.html` (`defaultSuccessUrl`).

Тестові користувачі з `keycloak/course-realm.json`: `admin`/`admin` (ролі `USER`, `ADMIN`) та `user`/`user` (роль `USER`).

### 5. JWT для API (AC5)
Ланцюжок `apiSecurityFilterChain` (`@Order(1)`, `securityMatcher("/api/**")`) — stateless, csrf вимкнено, `oauth2ResourceServer().jwt()`. Ролі витягаються з claim `realm_access.roles` у `CurrentUserAuthenticationConverter` і перетворюються на `ROLE_*` authorities; принципал — `record CurrentUser(username, roles)`. Запит із `Authorization: Bearer <token>` авторизується без сесії та UI. Без токена — `401` (`RestAuthEntryPoint`).

### 6. Обмеження на рівні SecurityFilterChain (AC6)
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated())
```
Звичайний користувач на `GET /api/admin/reports` отримує `403 Forbidden` (JSON від `RestAccessDeniedHandler`).

### 7. Обмеження через `@PreAuthorize` (AC7)
У `SecurityConfig` увімкнено `@EnableMethodSecurity(proxyTargetClass = true)`. Приклад:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/more")                 // AdminController, /api/reports/more
public String moreReports() { ... }
```
Шлях `/api/reports/**` у фільтр-чейні має лише `authenticated()`, тому блокування `USER` на `/api/reports/more` виконує саме метод-рівнева безпека (`AccessDeniedException` $\rightarrow$ `403`), незалежно від правил `SecurityFilterChain`. Також `@PreAuthorize` стоїть на методах `BookingController` (create — `USER`, update/delete — `ADMIN`).

### 8. Postman-колекція (AC8)
Файл `postman/lab1.postman_collection.json` містить 15 запитів у 5 групах:

| Група | Запити |
| :--- | :--- |
| **0. Tokens** | отримання токенів `user` та `admin` (Keycloak, password grant) |
| **1. JWT auth** | `/api/public/ping` $\rightarrow$ `200`; `/api/secure/me` без токена $\rightarrow$ `401`; з Bearer $\rightarrow$ `200` |
| **2. CUD** | Create (`201`), Create invalid (`400`), Read all (`200`), Update ADMIN (`200`), Update USER (`403`), Update not found (`404`), Delete USER (`403`), Delete ADMIN (`204`) |
| **3. SecurityFilterChain** | `/api/admin/reports`: USER $\rightarrow$ `403`, ADMIN $\rightarrow$ `200` |
| **4. @PreAuthorize** | `/api/reports`: USER $\rightarrow$ `200`; `/api/reports/more`: USER $\rightarrow$ `403`, ADMIN $\rightarrow$ `200` |

## Відповідність критеріям

| AC | Виконання | Де в коді |
| :--- | :--- | :--- |
| **1** | CUD + MapStruct | `booking/*` |
| **2** | Liquibase, `ddl-auto: validate` | `db/changelog/**` |
| **3** | UI | `/ui/index.html`, `/ui/bookings` |
| **4** | OIDC (Keycloak) | `uiSecurityFilterChain` |
| **5** | JWT Bearer | `apiSecurityFilterChain` |
| **6** | 403 на рівні фільтр-чейна | `/api/admin/**` $\rightarrow$ `hasRole("ADMIN")` |
| **7** | `@PreAuthorize` | `AdminController`, `BookingController` |
| **8** | Postman-колекція | `postman/lab1.postman_collection.json` |

## Висновок
Створено робочий CUD-застосунок із шаровою архітектурою, міграціями Liquibase та двома паралельними моделями автентифікації: сесійною (OIDC) для UI та stateless (JWT) для API. Доступ обмежується двома способами — URL-правилами у `SecurityFilterChain` та анотаціями `@PreAuthorize`. Готова Postman-колекція дозволяє швидко продемонструвати всі сценарії.